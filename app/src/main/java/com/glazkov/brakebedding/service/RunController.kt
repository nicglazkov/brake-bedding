package com.glazkov.brakebedding.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.glazkov.brakebedding.audio.Haptics
import com.glazkov.brakebedding.audio.VoiceCoach
import com.glazkov.brakebedding.data.AppSettings
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.Stage
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.engine.BeddingEngine
import com.glazkov.brakebedding.engine.RunEvent
import com.glazkov.brakebedding.engine.RunPhase
import com.glazkov.brakebedding.engine.RunState
import com.glazkov.brakebedding.location.LocationSpeedSource
import com.glazkov.brakebedding.location.SpeedReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The possible causes when the run screen does not get a usable speed. */
enum class SignalStatus {
    /** Speed data comes in. */
    OK,

    /** The app listens, but no data came in until now. */
    ACQUIRING,

    /** Speed data stopped during the run. */
    LOST,

    /** The location function of the device is off. */
    GPS_OFF,

    /** The user did not give access to the location. */
    NO_PERMISSION,

    /** The user gave access only to the approximate location. That is not sufficient. */
    COARSE_ONLY,
    ;

    val isUsable: Boolean get() = this == OK
}

/** The full state of the run. It is independent of all screens. */
data class RunSnapshot(
    val procedure: Procedure = Procedure(),
    val run: RunState = RunState(),
    val speedMps: Double = 0.0,
    val signal: SignalStatus = SignalStatus.ACQUIRING,
    val settings: AppSettings = AppSettings(),
) {
    val currentStage: Stage? get() = procedure.stages.getOrNull(run.stageIndex)
}

/**
 * The owner of the run: the engine, the tick loop, the speed source, and the cues.
 *
 * This is not in a ViewModel. This is intentional. The scope of a ViewModel stops with
 * its activity. A run in a ViewModel stops when Android removes the activity, with no
 * warning to the driver. This controller has application scope, and [RunService] keeps
 * the process alive around it. The ViewModel is only a window onto this state.
 */
// The static instance keeps a Context. Lint reports this as a possible leak. But
// get() only supplies the application context. That context lives as long as the
// process.
@Suppress("StaticFieldLeak")
class RunController private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val speedSource = LocationSpeedSource(context)
    private val voice = VoiceCoach(context)
    private val haptics = Haptics(context)

    private val _state = MutableStateFlow(RunSnapshot())
    val state: StateFlow<RunSnapshot> = _state.asStateFlow()

    private var engine = BeddingEngine(Procedure())
    private var latestFix: SpeedReading.Fix? = null
    private var providerDisabled = false
    private var tickJob: Job? = null
    private var speedJob: Job? = null

    init {
        observeSpeed()
        refreshSignal()
    }

    /** The stored procedure changed. An active engine keeps its copy until the run ends. */
    fun setProcedure(procedure: Procedure) {
        if (!_state.value.run.phase.isRunning) {
            engine = BeddingEngine(procedure)
        }
        _state.update { it.copy(procedure = procedure) }
    }

    fun setSettings(settings: AppSettings) {
        _state.update { it.copy(settings = settings) }
    }

    // --- Run control ------------------------------------------------------------------

    fun start() {
        val procedure = _state.value.procedure
        if (!procedure.isRunnable) return
        engine = BeddingEngine(procedure)
        applyEvent(RunEvent.Start)
        startTicking()
        // The service keeps this controller alive when the app is not on the screen.
        // The service monitors the state flow and stops itself when the run ends.
        val intent = Intent(context, RunService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun pause() = applyEvent(RunEvent.Pause)

    fun resume() = applyEvent(RunEvent.Resume)

    fun skipStage() = applyEvent(RunEvent.SkipStage)

    fun stop() {
        stopTicking()
        voice.stop()
        applyEvent(RunEvent.Stop)
    }

    fun togglePause() {
        if (_state.value.run.isPaused) resume() else pause()
    }

    // --- Speed and signal ---------------------------------------------------------------

    /**
     * Reads the permission state and the provider state again. The UI calls this after
     * the permission dialog and at each resume. This is necessary because the user can
     * give access in the system settings, and the app gets no message about it.
     */
    fun refreshPermissionState() {
        refreshSignal()
        if (speedSource.hasPermission()) observeSpeed()
    }

    /** Starts the collection of speed data. It replaces the last collector. */
    private fun observeSpeed() {
        speedJob?.cancel()
        speedJob = scope.launch {
            speedSource.readings().collect { reading ->
                when (reading) {
                    is SpeedReading.Fix -> {
                        providerDisabled = false
                        latestFix = reading
                    }

                    SpeedReading.ProviderDisabled -> {
                        providerDisabled = true
                        latestFix = null
                    }

                    SpeedReading.Acquiring -> providerDisabled = false
                }
                refreshSignal()
            }
        }
    }

    private fun refreshSignal() {
        val status = when {
            !speedSource.hasPermission() && speedSource.hasOnlyCoarsePermission() -> SignalStatus.COARSE_ONLY
            !speedSource.hasPermission() -> SignalStatus.NO_PERMISSION
            providerDisabled || !speedSource.isGpsEnabled() -> SignalStatus.GPS_OFF
            latestFix == null -> SignalStatus.ACQUIRING
            isFixStale() -> SignalStatus.LOST
            else -> SignalStatus.OK
        }
        val speed = if (status.isUsable) latestFix?.speedMps ?: 0.0 else 0.0
        _state.update { it.copy(signal = status, speedMps = speed) }
    }

    private fun isFixStale(): Boolean {
        val fix = latestFix ?: return true
        return SystemClock.elapsedRealtime() - fix.atElapsedRealtimeMillis > STALE_FIX_MS
    }

    // --- The tick loop --------------------------------------------------------------------

    private fun startTicking() {
        stopTicking()
        tickJob = scope.launch {
            var lastTickAt = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val now = SystemClock.elapsedRealtime()
                val deltaSeconds = (now - lastTickAt) / 1000.0
                lastTickAt = now

                refreshSignal()

                // If there is no good speed data, the run stops. It does not continue
                // with old data.
                val fix = latestFix
                if (fix == null || isFixStale()) continue

                applyEvent(RunEvent.Tick(fix.speedMps, deltaSeconds))
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun applyEvent(event: RunEvent) {
        val before = _state.value.run
        val after = engine.reduce(before, event)
        if (after == before) return
        _state.update { it.copy(run = after) }
        announceIfChanged(before, after)
        if (after.phase == RunPhase.FINISHED) stopTicking()
    }

    // --- Cues -------------------------------------------------------------------------

    private fun announceIfChanged(before: RunState, after: RunState) {
        val phaseChanged = before.phase != after.phase
        val stageChanged = before.stageIndex != after.stageIndex
        val cycleChanged = before.cycleIndex != after.cycleIndex
        if (!phaseChanged && !stageChanged && !cycleChanged) return

        val settings = _state.value.settings
        if (settings.hapticCues) {
            when {
                after.phase == RunPhase.BRAKE -> haptics.brakeNow()
                after.phase == RunPhase.FINISHED -> haptics.complete()
                phaseChanged -> haptics.phaseChange()
            }
        }
        if (settings.voiceCues && phaseChanged) {
            cueFor(after, settings.unitSystem)?.let(voice::say)
        }
    }

    /** The spoken cues obey ASD-STE100. The brake force names are technical names. */
    private fun cueFor(state: RunState, units: UnitSystem): String? {
        val stage = engine.currentStage(state)
        return when (state.phase) {
            RunPhase.SPEED_UP -> (stage as? BeddingStage)?.let {
                "Increase speed to ${units.formatSpeed(it.startSpeedMps)}"
            }

            RunPhase.SLOW_DOWN -> (stage as? BeddingStage)?.let {
                "Decrease speed to ${units.formatSpeed(it.startSpeedMps)}"
            }

            RunPhase.HOLD -> "Hold this speed"

            RunPhase.BRAKE -> (stage as? BeddingStage)?.let {
                "${it.brakingIntensity.shortName} braking, now"
            } ?: "Brake now"

            RunPhase.GAP -> "Drive. Do not brake."

            RunPhase.COOLDOWN -> (stage as? CooldownStage)?.let {
                "Cooldown. Drive ${units.formatDistance(it.distanceMeters)} " +
                    "${units.distanceLabel} with minimum braking."
            } ?: "Cooldown"

            RunPhase.FINISHED -> "The procedure is complete. Let the brakes become cool before you park."
            RunPhase.IDLE -> null
        }
    }

    companion object {
        /**
         * Four ticks each second. GPS supplies approximately one sample each second.
         * The higher tick rate keeps the countdown and the distance displays smooth.
         */
        const val TICK_INTERVAL_MS = 250L

        /** The maximum age of speed data that the run accepts. */
        const val STALE_FIX_MS = 3_000L

        @Volatile
        private var instance: RunController? = null

        /** One controller for each process. The UI and the service use this one object. */
        fun get(context: Context): RunController =
            instance ?: synchronized(this) {
                instance ?: RunController(context.applicationContext).also { instance = it }
            }
    }
}
