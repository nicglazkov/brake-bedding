package com.glazkov.brakebedding.engine

/**
 * The instruction for the driver at this time.
 *
 * The phases are simple. This is intentional. The driver looks at the screen for a
 * very short time. More phases would not be readable in that time.
 */
enum class RunPhase {
    /** No run is active. */
    IDLE,

    /** The speed is below the start speed of the stage. */
    SPEED_UP,

    /** The speed is too far above the start speed of the stage. */
    SLOW_DOWN,

    /** The speed is in the permitted band. The countdown to the stop is active. */
    HOLD,

    /** The driver brakes to the target speed of the stage. */
    BRAKE,

    /** The driver drives the distance between stops. The brakes become more cool. */
    GAP,

    /** The last stage: the cooldown. */
    COOLDOWN,

    /** All stages are complete. */
    FINISHED,
    ;

    val isRunning: Boolean get() = this != IDLE && this != FINISHED
}

/**
 * The full state of a run.
 *
 * All data that the engine uses is here. Because of this, [BeddingEngine] can be a
 * pure function, and the state can stay in memory through a screen rotation.
 */
data class RunState(
    val phase: RunPhase = RunPhase.IDLE,
    val stageIndex: Int = 0,
    /** The index of the stop in the bedding stage. The first stop is zero. */
    val cycleIndex: Int = 0,
    /** The seconds of hold time that remain before the stop starts. */
    val holdSecondsRemaining: Double = BeddingEngine.HOLD_SECONDS,
    /** The distance that remains in the gap or in the cooldown. */
    val remainingMeters: Double = 0.0,
    /** The number of complete stops in the full procedure. The progress bar uses this. */
    val completedStops: Int = 0,
    val isPaused: Boolean = false,
    val elapsedSeconds: Double = 0.0,
    val distanceTraveledMeters: Double = 0.0,
) {
    val isActive: Boolean get() = phase.isRunning && !isPaused
}
