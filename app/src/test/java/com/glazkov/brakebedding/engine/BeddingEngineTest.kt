package com.glazkov.brakebedding.engine

import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.BrakingIntensity
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.Units.milesToMeters
import com.glazkov.brakebedding.data.Units.mphToMps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeddingEngineTest {

    private val stage = BeddingStage(
        numberOfStops = 3,
        startSpeedMps = mphToMps(40.0),
        targetSpeedMps = mphToMps(15.0),
        gapDistanceMeters = milesToMeters(0.25),
        brakingIntensity = BrakingIntensity.LIGHT,
    )

    private val cooldown = CooldownStage(distanceMeters = milesToMeters(2.0))

    private val engine = BeddingEngine(Procedure("test", listOf(stage, cooldown)))

    // --- Reaching and holding the start speed -------------------------------------

    @Test
    fun `start enters the first stage asking for speed`() {
        val state = engine.reduce(RunState(), RunEvent.Start)
        assertEquals(RunPhase.SPEED_UP, state.phase)
        assertEquals(0, state.stageIndex)
        assertEquals(0, state.cycleIndex)
    }

    @Test
    fun `below the start speed keeps asking for speed`() {
        val state = engine.start().drive(mph = 20.0, seconds = 5.0)
        assertEquals(RunPhase.SPEED_UP, state.phase)
    }

    @Test
    fun `reaching the start speed begins the hold`() {
        val state = engine.start().tick(mph = 40.0)
        assertEquals(RunPhase.HOLD, state.phase)
        assertEquals(BeddingEngine.HOLD_SECONDS, state.holdSecondsRemaining, 0.001)
    }

    @Test
    fun `holding for the full countdown starts the stop`() {
        val state = engine.start().drive(mph = 40.0, seconds = 3.5)
        assertEquals(RunPhase.BRAKE, state.phase)
    }

    /**
     * The previous implementation posted a new Handler countdown on every entry into the
     * hold state without cancelling the old one, so wobbling across the tolerance band
     * left several countdowns running and the stop began early.
     */
    @Test
    fun `dropping out of the band restarts the countdown from the beginning`() {
        var state = engine.start().drive(mph = 40.0, seconds = 2.0)
        assertEquals(RunPhase.HOLD, state.phase)
        assertTrue(state.holdSecondsRemaining < BeddingEngine.HOLD_SECONDS)

        state = state.drive(mph = 30.0, seconds = 0.5)
        assertEquals(RunPhase.SPEED_UP, state.phase)
        assertEquals(BeddingEngine.HOLD_SECONDS, state.holdSecondsRemaining, 0.001)

        // Back in the band, but only for two seconds, so the stop must not begin yet.
        state = state.drive(mph = 40.0, seconds = 2.0)
        assertEquals(RunPhase.HOLD, state.phase)
    }

    @Test
    fun `wobbling in and out of the band never begins the stop early`() {
        var state = engine.start()
        repeat(20) {
            state = state.drive(mph = 40.0, seconds = 2.0)
            state = state.drive(mph = 30.0, seconds = 0.5)
        }
        // Twenty near-misses, none of which should have accumulated into a stop.
        assertEquals(0, state.completedStops)
    }

    /**
     * Being slightly over the start speed used to fall through the classification into
     * `else -> ACCELERATING`, telling the driver to speed up while already too fast.
     */
    @Test
    fun `slightly over the start speed counts as at speed`() {
        val state = engine.start().tick(mph = 43.0)
        assertEquals(RunPhase.HOLD, state.phase)
    }

    @Test
    fun `well over the start speed asks the driver to slow down`() {
        val state = engine.start().tick(mph = 47.0)
        assertEquals(RunPhase.SLOW_DOWN, state.phase)
    }

    // --- Stops, gaps and stage transitions ----------------------------------------

    @Test
    fun `braking to the target speed opens the gap`() {
        val state = engine.start()
            .drive(mph = 40.0, seconds = 3.5)
            .tick(mph = 14.0)
        assertEquals(RunPhase.GAP, state.phase)
        assertEquals(1, state.completedStops)
        assertEquals(stage.gapDistanceMeters, state.remainingMeters, 0.001)
    }

    @Test
    fun `driving the gap advances to the next stop`() {
        val state = engine.start()
            .drive(mph = 40.0, seconds = 3.5)
            .tick(mph = 14.0)
            .drive(mph = 30.0, seconds = 60.0)
        assertEquals(RunPhase.SPEED_UP, state.phase)
        assertEquals(1, state.cycleIndex)
    }

    @Test
    fun `a zero gap distance moves straight to the next stop`() {
        val noGap = stage.copy(gapDistanceMeters = 0.0)
        val local = BeddingEngine(Procedure("test", listOf(noGap)))
        val state = local.reduce(RunState(), RunEvent.Start)
            .let { local.drive(it, mph = 40.0, seconds = 3.5) }
            .let { local.tick(it, mph = 14.0) }
        assertEquals(RunPhase.SPEED_UP, state.phase)
        assertEquals(1, state.cycleIndex)
    }

    @Test
    fun `completing every stop moves to the next stage`() {
        var state = engine.start()
        repeat(stage.numberOfStops) {
            state = state.completeOneStop()
        }
        assertEquals(1, state.stageIndex)
        assertEquals(RunPhase.COOLDOWN, state.phase)
    }

    // --- The cooldown stage --------------------------------------------------------

    /**
     * The headline bug. Cooldown stages were written through a polymorphic Gson adapter
     * but read back as `List<BeddingStage>`, so every cooldown arrived with all fields
     * zeroed and the app instructed the driver to "SLOW DOWN to 0 mph" instead of
     * running a cooldown.
     */
    @Test
    fun `a cooldown stage runs as a cooldown`() {
        var state = engine.start()
        repeat(stage.numberOfStops) { state = state.completeOneStop() }

        assertEquals(RunPhase.COOLDOWN, state.phase)
        assertEquals(cooldown.distanceMeters, state.remainingMeters, 0.001)

        state = state.drive(mph = 45.0, seconds = 30.0)
        assertEquals(RunPhase.COOLDOWN, state.phase)
        assertTrue("cooldown distance should be counting down", state.remainingMeters < cooldown.distanceMeters)
    }

    @Test
    fun `driving the full cooldown distance finishes the procedure`() {
        var state = engine.start()
        repeat(stage.numberOfStops) { state = state.completeOneStop() }
        state = state.drive(mph = 45.0, seconds = 60.0 * 5)
        assertEquals(RunPhase.FINISHED, state.phase)
    }

    @Test
    fun `a procedure that is only a cooldown still runs`() {
        val local = BeddingEngine(Procedure("cooldown only", listOf(cooldown)))
        var state = local.reduce(RunState(), RunEvent.Start)
        assertEquals(RunPhase.COOLDOWN, state.phase)
        state = local.drive(state, mph = 45.0, seconds = 60.0 * 5)
        assertEquals(RunPhase.FINISHED, state.phase)
    }

    // --- Run control ---------------------------------------------------------------

    @Test
    fun `pausing freezes progress`() {
        val running = engine.start().drive(mph = 40.0, seconds = 2.0)
        val paused = engine.reduce(running, RunEvent.Pause)
        val afterTicks = paused.drive(mph = 40.0, seconds = 10.0)
        assertEquals(paused.holdSecondsRemaining, afterTicks.holdSecondsRemaining, 0.001)
        assertEquals(RunPhase.HOLD, afterTicks.phase)
    }

    @Test
    fun `resuming continues from where it paused`() {
        val paused = engine.reduce(engine.start().drive(mph = 40.0, seconds = 2.0), RunEvent.Pause)
        val resumed = engine.reduce(paused, RunEvent.Resume).drive(mph = 40.0, seconds = 2.0)
        assertEquals(RunPhase.BRAKE, resumed.phase)
    }

    @Test
    fun `stopping returns to idle`() {
        val state = engine.reduce(engine.start().drive(mph = 40.0, seconds = 2.0), RunEvent.Stop)
        assertEquals(RunPhase.IDLE, state.phase)
        assertEquals(0, state.completedStops)
    }

    @Test
    fun `skipping a stage moves to the next one`() {
        val state = engine.reduce(engine.start(), RunEvent.SkipStage)
        assertEquals(1, state.stageIndex)
        assertEquals(RunPhase.COOLDOWN, state.phase)
    }

    @Test
    fun `skipping the last stage finishes the procedure`() {
        val state = engine.reduce(
            engine.reduce(engine.start(), RunEvent.SkipStage),
            RunEvent.SkipStage,
        )
        assertEquals(RunPhase.FINISHED, state.phase)
    }

    /**
     * A backgrounded app can deliver one tick covering many seconds. Without clamping,
     * a single late sample would consume an entire gap.
     */
    @Test
    fun `one very long tick cannot consume a whole gap`() {
        val inGap = engine.start().drive(mph = 40.0, seconds = 3.5).tick(mph = 14.0)
        val after = engine.reduce(inGap, RunEvent.Tick(mphToMps(60.0), deltaSeconds = 600.0))
        assertEquals(RunPhase.GAP, after.phase)
        assertTrue(after.remainingMeters > 0)
    }

    @Test
    fun `a negative speed sample is treated as stopped`() {
        val state = engine.start().tick(mph = -5.0)
        assertEquals(RunPhase.SPEED_UP, state.phase)
        assertEquals(0.0, state.distanceTraveledMeters, 0.001)
    }

    // --- Whole-procedure simulation -------------------------------------------------

    /**
     * Runs a realistic procedure against a driver that actually follows the
     * instructions, which catches transitions that only appear in sequence.
     */
    @Test
    fun `a simulated driver completes a full procedure`() {
        val procedure = Procedure(
            name = "full",
            stages = listOf(
                BeddingStage(
                    numberOfStops = 20,
                    startSpeedMps = mphToMps(42.0),
                    targetSpeedMps = mphToMps(18.0),
                    gapDistanceMeters = milesToMeters(0.30),
                    brakingIntensity = BrakingIntensity.LIGHT,
                ),
                BeddingStage(
                    numberOfStops = 10,
                    startSpeedMps = mphToMps(54.0),
                    targetSpeedMps = mphToMps(30.0),
                    gapDistanceMeters = milesToMeters(0.62),
                    brakingIntensity = BrakingIntensity.MODERATE,
                ),
                CooldownStage(distanceMeters = milesToMeters(6.0)),
            ),
        )
        val sim = BeddingEngine(procedure)
        var state = sim.reduce(RunState(), RunEvent.Start)
        var speed = 0.0
        val dt = 0.25

        var ticks = 0
        val limit = 4 * 60 * 60 * 4 // four simulated hours at 4 Hz
        while (state.phase != RunPhase.FINISHED && ticks < limit) {
            speed = nextSpeed(sim, state, speed, dt)
            state = sim.reduce(state, RunEvent.Tick(speed, dt))
            ticks++
        }

        assertEquals("procedure did not finish", RunPhase.FINISHED, state.phase)
        assertEquals("every stop should be counted", 30, state.completedStops)
        assertTrue("should have covered real distance", state.distanceTraveledMeters > milesToMeters(20.0))
    }

    /** A driver that responds to whatever the app is currently asking for. */
    private fun nextSpeed(engine: BeddingEngine, state: RunState, current: Double, dt: Double): Double {
        val accel = 2.5 * dt   // roughly 0-60 in eleven seconds
        val braking = 5.0 * dt // a firm but unremarkable stop
        val cruise = mphToMps(45.0)
        return when (state.phase) {
            RunPhase.SPEED_UP -> current + accel
            RunPhase.SLOW_DOWN -> (current - braking).coerceAtLeast(0.0)
            RunPhase.HOLD -> current
            RunPhase.BRAKE -> (current - braking).coerceAtLeast(0.0)
            RunPhase.GAP, RunPhase.COOLDOWN ->
                if (current < cruise) current + accel else cruise
            else -> current
        }
    }

    // --- helpers --------------------------------------------------------------------

    private fun BeddingEngine.start(): RunState = reduce(RunState(), RunEvent.Start)

    private fun RunState.tick(mph: Double, dt: Double = 0.25): RunState =
        engine.reduce(this, RunEvent.Tick(mphToMps(mph), dt))

    private fun BeddingEngine.tick(state: RunState, mph: Double, dt: Double = 0.25): RunState =
        reduce(state, RunEvent.Tick(mphToMps(mph), dt))

    private fun RunState.drive(mph: Double, seconds: Double, dt: Double = 0.25): RunState =
        engine.drive(this, mph, seconds, dt)

    private fun BeddingEngine.drive(
        state: RunState,
        mph: Double,
        seconds: Double,
        dt: Double = 0.25,
    ): RunState {
        var current = state
        var elapsed = 0.0
        while (elapsed < seconds) {
            current = reduce(current, RunEvent.Tick(mphToMps(mph), dt))
            elapsed += dt
        }
        return current
    }

    /**
     * Drives one complete stop: up to speed, hold, brake, then out through the gap,
     * stopping the moment the gap ends so the caller can inspect the state at exactly
     * that transition rather than somewhere past it.
     */
    private fun RunState.completeOneStop(): RunState {
        var state = drive(mph = 40.0, seconds = 3.5).tick(mph = 14.0)
        var guard = 0
        while (state.phase == RunPhase.GAP && guard++ < 100_000) {
            state = state.tick(mph = 35.0)
        }
        return state
    }
}
