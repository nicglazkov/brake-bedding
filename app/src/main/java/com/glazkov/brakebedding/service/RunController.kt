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

/** Why the run screen might not be receiving usable speed. */
enum class SignalStatus {
    /** Fixes are arriving. */
    OK,

    /** Listening, nothing yet. */
    ACQUIRING,

    /** Fixes stopped arriving mid-run. */
    LOST,

    /** Location services are switched off. */
    GPS_OFF,

    /** The user has not granted location access. */
    NO_PERMISSION,

    /** Only approximate location was granted, which is not precise enough. */
    COARSE_ONLY,
    ;

    val isUsable: Boolean get() = this == OK
}

/** Everything the run currently is, independent of any screen. */
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
 * Owns the run: the engine, the tick loop, the speed feed and the cues.
 *
 * This is deliberately not inside a ViewModel. A ViewModel's scope dies with its
 * activity, so a run driven from there stops ticking the moment the app is backgrounded
 * long enough to be destroyed — mid-procedure, at speed, with no warning. The controller
 * is application-scoped and [RunService] holds the process alive around it; the
 * ViewModel is reduced to a window onto this state.
 */
// The static instance holds a Context, which lint flags as a leak; get() only ever
// passes the application context here, which lives as long as the process regardless.
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

    /** The stored procedure changed; a running engine keeps its own copy until it ends. */
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
        // The service exists to keep this controller alive and visible while the app is
        // backgrounded; it observes the state flow and stops itself when the run ends.
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
     * Re-reads permission and provider state; called from the UI after the permission
     * dialog resolves and on every resume, since location can be granted from system
     * settings without the app hearing about it.
     */
    fun refreshPermissionState() {
        refreshSignal()
        if (speedSource.hasPermission()) observeSpeed()
    }

    /** Starts collecting fixes, replacing any existing collector. */
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

                // Without a trustworthy fix the run freezes rather than integrating a
                // stale speed.
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

    private fun cueFor(state: RunState, units: UnitSystem): String? {
        val stage = engine.currentStage(state)
        return when (state.phase) {
            RunPhase.SPEED_UP -> (stage as? BeddingStage)?.let {
                "Speed up to ${units.formatSpeed(it.startSpeedMps)}"
            }

            RunPhase.SLOW_DOWN -> (stage as? BeddingStage)?.let {
                "Slow to ${units.formatSpeed(it.startSpeedMps)}"
            }

            RunPhase.HOLD -> "Hold"

            RunPhase.BRAKE -> (stage as? BeddingStage)?.let {
                "${it.brakingIntensity.shortName} braking, now"
            } ?: "Brake now"

            RunPhase.GAP -> "Coast"

            RunPhase.COOLDOWN -> (stage as? CooldownStage)?.let {
                "Cool down. ${units.formatDistance(it.distanceMeters)} ${units.distanceLabel}."
            } ?: "Cool down"

            RunPhase.FINISHED -> "Procedure complete. Let the brakes cool before parking."
            RunPhase.IDLE -> null
        }
    }

    companion object {
        /**
         * Four samples a second. GPS itself only produces about one, but ticking faster
         * keeps the countdown and distance readouts smooth between fixes.
         */
        const val TICK_INTERVAL_MS = 250L

        /** How old a fix may be before the run stops trusting it. */
        const val STALE_FIX_MS = 3_000L

        @Volatile
        private var instance: RunController? = null

        /** One controller per process; both the UI and the service talk to this one. */
        fun get(context: Context): RunController =
            instance ?: synchronized(this) {
                instance ?: RunController(context.applicationContext).also { instance = it }
            }
    }
}
