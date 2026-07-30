package com.glazkov.brakebedding.engine

import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.Stage
import com.glazkov.brakebedding.data.Units

/** The events that can move a run forward. */
sealed interface RunEvent {
    data object Start : RunEvent
    data object Pause : RunEvent
    data object Resume : RunEvent
    data object Stop : RunEvent

    /** Goes to the subsequent stage. Use this event when the road is too short. */
    data object SkipStage : RunEvent

    /**
     * One speed sample. [deltaSeconds] is the time after the last tick. The engine
     * multiplies the speed by this time to calculate the distance.
     */
    data class Tick(val speedMps: Double, val deltaSeconds: Double) : RunEvent
}

/**
 * Moves a [Procedure] forward from speed samples.
 *
 * This class is pure. It keeps no data, it does not use Android, and it makes no
 * callbacks. A full run is one fold of events into states. Because of this, tests do
 * not need a device. Also, the timer races of the first implementation are not
 * possible.
 */
class BeddingEngine(val procedure: Procedure) {

    /** Applies one event and returns the new state. It does not change [state]. */
    fun reduce(state: RunState, event: RunEvent): RunState = when (event) {
        RunEvent.Start -> enterStage(RunState(), 0)
        RunEvent.Stop -> RunState()
        RunEvent.Pause -> if (state.phase.isRunning) state.copy(isPaused = true) else state
        RunEvent.Resume -> state.copy(isPaused = false)
        RunEvent.SkipStage ->
            if (state.phase.isRunning) enterStage(state, state.stageIndex + 1) else state

        is RunEvent.Tick -> tick(state, event)
    }

    fun stageAt(index: Int): Stage? = procedure.stages.getOrNull(index)

    fun currentStage(state: RunState): Stage? = stageAt(state.stageIndex)

    private fun tick(state: RunState, event: RunEvent.Tick): RunState {
        if (!state.isActive) return state

        // A stopped app or a stopped GPS can cause a large time between samples. The
        // limit prevents one late tick that moves the driver through a full gap.
        val dt = event.deltaSeconds.coerceIn(0.0, MAX_TICK_SECONDS)
        val speed = event.speedMps.coerceAtLeast(0.0)

        val advanced = state.copy(
            elapsedSeconds = state.elapsedSeconds + dt,
            distanceTraveledMeters = state.distanceTraveledMeters + speed * dt,
        )

        return when (val stage = currentStage(state)) {
            is BeddingStage -> tickBedding(advanced, stage, speed, dt)
            is CooldownStage -> tickCooldown(advanced, speed, dt)
            null -> advanced.copy(phase = RunPhase.FINISHED)
        }
    }

    private fun tickBedding(
        state: RunState,
        stage: BeddingStage,
        speed: Double,
        dt: Double,
    ): RunState = when (state.phase) {
        RunPhase.SPEED_UP, RunPhase.SLOW_DOWN, RunPhase.HOLD -> approach(state, stage, speed, dt)
        RunPhase.BRAKE -> if (speed <= stage.targetSpeedMps) finishStop(state, stage) else state
        RunPhase.GAP -> {
            val remaining = state.remainingMeters - speed * dt
            if (remaining <= 0) advanceCycle(state, stage) else state.copy(remainingMeters = remaining)
        }
        // A cooldown phase with a bedding stage shows that the procedure changed
        // during the run. Start the stage again.
        else -> enterStage(state, state.stageIndex)
    }

    /**
     * Gets the vehicle to the start speed of the stage and holds it there.
     *
     * The speed band is not symmetrical. This is intentional. A speed that is a small
     * amount too high gives the stage the necessary energy. The engine accepts it. A
     * speed that is too low does not give that energy. The first implementation had
     * no category for 2 to 5 mph above the start speed. It told the driver to
     * increase the speed.
     */
    private fun approach(state: RunState, stage: BeddingStage, speed: Double, dt: Double): RunState {
        val diff = speed - stage.startSpeedMps
        return when {
            diff < -SPEED_TOLERANCE_MPS ->
                state.copy(phase = RunPhase.SPEED_UP, holdSecondsRemaining = HOLD_SECONDS)

            diff > SPEED_OVERAGE_MPS ->
                state.copy(phase = RunPhase.SLOW_DOWN, holdSecondsRemaining = HOLD_SECONDS)

            state.phase != RunPhase.HOLD ->
                state.copy(phase = RunPhase.HOLD, holdSecondsRemaining = HOLD_SECONDS)

            else -> {
                val remaining = state.holdSecondsRemaining - dt
                if (remaining <= 0) {
                    state.copy(phase = RunPhase.BRAKE, holdSecondsRemaining = 0.0)
                } else {
                    state.copy(holdSecondsRemaining = remaining)
                }
            }
        }
    }

    /** The vehicle is at the target speed. The stop is complete. */
    private fun finishStop(state: RunState, stage: BeddingStage): RunState {
        val counted = state.copy(completedStops = state.completedStops + 1)
        return if (stage.gapDistanceMeters > 0) {
            counted.copy(phase = RunPhase.GAP, remainingMeters = stage.gapDistanceMeters)
        } else {
            advanceCycle(counted, stage)
        }
    }

    private fun advanceCycle(state: RunState, stage: BeddingStage): RunState {
        val nextCycle = state.cycleIndex + 1
        return if (nextCycle >= stage.numberOfStops) {
            enterStage(state, state.stageIndex + 1)
        } else {
            state.copy(
                phase = RunPhase.SPEED_UP,
                cycleIndex = nextCycle,
                holdSecondsRemaining = HOLD_SECONDS,
                remainingMeters = 0.0,
            )
        }
    }

    private fun tickCooldown(state: RunState, speed: Double, dt: Double): RunState {
        val remaining = state.remainingMeters - speed * dt
        return if (remaining <= 0) {
            enterStage(state, state.stageIndex + 1)
        } else {
            state.copy(phase = RunPhase.COOLDOWN, remainingMeters = remaining)
        }
    }

    /**
     * Goes to the stage at [index]. If there is no stage there, the run is complete.
     *
     * A bedding stage starts in [RunPhase.SPEED_UP] as an initial value. The
     * subsequent tick sets the correct phase from the speed. At 4 Hz, the driver does
     * not see the initial value.
     */
    private fun enterStage(state: RunState, index: Int): RunState {
        val stage = stageAt(index) ?: return state.copy(
            phase = RunPhase.FINISHED,
            remainingMeters = 0.0,
        )
        val base = state.copy(
            stageIndex = index,
            cycleIndex = 0,
            holdSecondsRemaining = HOLD_SECONDS,
        )
        return when (stage) {
            is BeddingStage -> base.copy(phase = RunPhase.SPEED_UP, remainingMeters = 0.0)
            is CooldownStage ->
                base.copy(phase = RunPhase.COOLDOWN, remainingMeters = stage.distanceMeters)
        }
    }

    companion object {
        /** The time at the start speed that is necessary before a stop starts. */
        const val HOLD_SECONDS = 3.0

        /** The permitted difference below the start speed. */
        val SPEED_TOLERANCE_MPS = Units.mphToMps(2.0)

        /** The permitted difference above the start speed. */
        val SPEED_OVERAGE_MPS = Units.mphToMps(5.0)

        /** The maximum time that one tick can supply. */
        const val MAX_TICK_SECONDS = 2.0
    }
}
