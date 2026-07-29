package com.glazkov.brakebedding.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glazkov.brakebedding.audio.Haptics
import com.glazkov.brakebedding.audio.VoiceCoach
import com.glazkov.brakebedding.data.AppSettings
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.ProcedureRepository
import com.glazkov.brakebedding.data.SettingsRepository
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.engine.BeddingEngine
import com.glazkov.brakebedding.engine.RunEvent
import com.glazkov.brakebedding.engine.RunPhase
import com.glazkov.brakebedding.engine.RunState
import com.glazkov.brakebedding.location.LocationSpeedSource
import com.glazkov.brakebedding.location.SpeedReading
import kotlinx.coroutines.Job
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

data class RunUiState(
    val procedure: Procedure = Procedure(),
    val settings: AppSettings = AppSettings(),
    val run: RunState = RunState(),
    val speedMps: Double = 0.0,
    val signal: SignalStatus = SignalStatus.ACQUIRING,
) {
    val currentStage get() = procedure.stages.getOrNull(run.stageIndex)
    val currentBeddingStage get() = currentStage as? BeddingStage
}

class RunViewModel(
    private val procedureRepository: ProcedureRepository,
    private val settingsRepository: SettingsRepository,
    private val speedSource: LocationSpeedSource,
    private val voice: VoiceCoach,
    private val haptics: Haptics,
) : ViewModel() {

    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state.asStateFlow()

    private var engine = BeddingEngine(Procedure())
    private var latestFix: SpeedReading.Fix? = null
    private var providerDisabled = false
    private var tickJob: Job? = null
    private var speedJob: Job? = null

    init {
        viewModelScope.launch {
            procedureRepository.migrateLegacyDataIfNeeded()
        }
        viewModelScope.launch {
            procedureRepository.procedure.collect { procedure ->
                // A procedure edited mid-run must not swap out from under the engine, so
                // the running one is kept until the run ends.
                if (!_state.value.run.phase.isRunning) {
                    engine = BeddingEngine(procedure)
                }
                _state.update { it.copy(procedure = procedure) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        observeSpeed()
        refreshPermissionState()
    }

    /**
     * Starts collecting fixes, replacing any existing collector.
     *
     * Guarded because this is called both at construction and every time the permission
     * state is re-checked; without the guard each check would leave another collector
     * running against the same provider.
     */
    private fun observeSpeed() {
        speedJob?.cancel()
        speedJob = viewModelScope.launch {
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

    /**
     * Re-reads permission and provider state.
     *
     * Called after the permission dialog resolves and on every resume, since the user
     * can grant location or switch GPS on from system settings and come back without the
     * app hearing about it.
     */
    fun refreshPermissionState() {
        refreshSignal()
        if (speedSource.hasPermission()) observeSpeed()
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

    // --- Run control ------------------------------------------------------------------

    fun start() {
        val procedure = _state.value.procedure
        if (!procedure.isRunnable) return
        engine = BeddingEngine(procedure)
        applyEvent(RunEvent.Start)
        startTicking()
    }

    fun pause() = applyEvent(RunEvent.Pause)

    fun resume() = applyEvent(RunEvent.Resume)

    fun skipStage() = applyEvent(RunEvent.SkipStage)

    fun stop() {
        stopTicking()
        voice.stop()
        applyEvent(RunEvent.Stop)
    }

    private fun startTicking() {
        stopTicking()
        tickJob = viewModelScope.launch {
            var lastTickAt = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val now = SystemClock.elapsedRealtime()
                val deltaSeconds = (now - lastTickAt) / 1000.0
                lastTickAt = now

                refreshSignal()

                // Without a trustworthy fix the run freezes rather than integrating a
                // stale speed. The previous version kept using the last known value
                // indefinitely, so a tunnel would "drive" the cooldown while parked.
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

    override fun onCleared() {
        stopTicking()
        voice.shutdown()
    }

    companion object {
        /**
         * Four samples a second. GPS itself only produces about one, but ticking faster
         * keeps the countdown and distance readouts smooth between fixes.
         */
        const val TICK_INTERVAL_MS = 250L

        /** How old a fix may be before the run stops trusting it. */
        const val STALE_FIX_MS = 3_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                RunViewModel(
                    procedureRepository = ProcedureRepository(app),
                    settingsRepository = SettingsRepository(app),
                    speedSource = LocationSpeedSource(app),
                    voice = VoiceCoach(app),
                    haptics = Haptics(app),
                )
            }
        }
    }
}
