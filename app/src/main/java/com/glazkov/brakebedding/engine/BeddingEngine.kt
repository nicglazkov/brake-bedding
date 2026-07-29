package com.glazkov.brakebedding.engine

import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.Stage
import com.glazkov.brakebedding.data.Units

/** Everything that can move a run forwards. */
sealed interface RunEvent {
    data object Start : RunEvent
    data object Pause : RunEvent
    data object Resume : RunEvent
    data object Stop : RunEvent

    /** Skips the rest of the current stage, for when a road runs out. */
    data object SkipStage : RunEvent

    /**
     * One speed sample. [deltaSeconds] is the wall time since the previous tick, which
     * is what distances are integrated against.
     */
    data class Tick(val speedMps: Double, val deltaSeconds: Double) : RunEvent
}

/**
 * Drives a [Procedure] forwards from speed samples.
 *
 * This class is pure: it holds no mutable state, touches nothing on Android, and posts
 * no callbacks. A whole run is `states.fold(initial, engine::reduce)`, which is what
 * makes it testable without a device and what removes the timer races the previous
 * Handler-based implementation had.
 */
class BeddingEngine(val procedure: Procedure) {

    /** Applies one event, returning the resulting state. Never mutates [state]. */
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

        // A backgrounded app or a stalled GPS can produce a huge gap between samples.
        // Clamping stops one late tick from teleporting the driver through a gap.
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
        // A cooldown phase against a bedding stage means the procedure was edited mid-run.
        else -> enterStage(state, state.stageIndex)
    }

    /**
     * Gets the car to the stage's start speed and holds it there.
     *
     * The acceptance band is asymmetric on purpose: being a little fast still delivers
     * the energy the stage is asking for, so it is accepted, while being slow does not.
     * The previous implementation left 2-5 mph over the start speed unclassified and
     * fell through to telling the driver to accelerate.
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

    /** The driver has reached the target speed, so the stop itself is done. */
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
     * Moves to [index], or finishes if the procedure has run out of stages.
     *
     * Bedding stages start in [RunPhase.SPEED_UP] provisionally; the next tick
     * reclassifies against the actual speed, which at 4 Hz the driver never sees.
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
        /** How long the car must sit at the start speed before a stop begins. */
        const val HOLD_SECONDS = 3.0

        /** How far under the start speed still counts as "at speed". */
        val SPEED_TOLERANCE_MPS = Units.mphToMps(2.0)

        /** How far over the start speed is tolerated before asking the driver to back off. */
        val SPEED_OVERAGE_MPS = Units.mphToMps(5.0)

        /** Upper bound applied to a single tick's elapsed time. */
        const val MAX_TICK_SECONDS = 2.0
    }
}
