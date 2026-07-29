package com.glazkov.brakebedding.engine

/**
 * What the driver should be doing right now.
 *
 * Phases are deliberately coarse. Anything finer would be unreadable at a glance from
 * the driver's seat, which is the only place this screen is ever looked at.
 */
enum class RunPhase {
    /** Nothing running. */
    IDLE,

    /** Below the stage's start speed. */
    SPEED_UP,

    /** More than the allowance above the stage's start speed. */
    SLOW_DOWN,

    /** Within the acceptance band, counting down before the stop. */
    HOLD,

    /** Braking towards the stage's target speed. */
    BRAKE,

    /** Coasting the gap distance between stops so the brakes shed heat. */
    GAP,

    /** Running the final cooldown stage. */
    COOLDOWN,

    /** Every stage is done. */
    FINISHED,
    ;

    val isRunning: Boolean get() = this != IDLE && this != FINISHED
}

/**
 * The complete state of a run.
 *
 * Everything the engine needs lives here, which is what lets [BeddingEngine] be a pure
 * function and lets the whole thing survive a rotation by sitting in a ViewModel.
 */
data class RunState(
    val phase: RunPhase = RunPhase.IDLE,
    val stageIndex: Int = 0,
    /** Zero-based index of the stop within the current bedding stage. */
    val cycleIndex: Int = 0,
    /** Seconds still to hold before the stop begins. */
    val holdSecondsRemaining: Double = BeddingEngine.HOLD_SECONDS,
    /** Distance left in the current gap or cooldown. */
    val remainingMeters: Double = 0.0,
    /** Stops completed across the whole procedure, for the progress indicator. */
    val completedStops: Int = 0,
    val isPaused: Boolean = false,
    val elapsedSeconds: Double = 0.0,
    val distanceTraveledMeters: Double = 0.0,
) {
    val isActive: Boolean get() = phase.isRunning && !isPaused
}
