package com.glazkov.brakebedding.location

import kotlinx.coroutines.flow.Flow

/** What the location stack currently knows about how fast the car is moving. */
sealed interface SpeedReading {

    /** Listening, but no usable fix has arrived yet. */
    data object Acquiring : SpeedReading

    /** Location services are switched off, so there is nothing to wait for. */
    data object ProviderDisabled : SpeedReading

    /**
     * A speed fix.
     *
     * [atElapsedRealtimeMillis] comes from the monotonic clock rather than wall time.
     * Wall time can step backwards when the network corrects the clock, which would make
     * a fresh fix look stale and freeze a run mid-procedure.
     */
    data class Fix(
        val speedMps: Double,
        val accuracyMps: Float?,
        val atElapsedRealtimeMillis: Long,
    ) : SpeedReading
}

/**
 * Where speed comes from.
 *
 * Behind an interface so a run can be driven by synthetic samples in tests and, during
 * development, by the emulator's mock location without the engine knowing the difference.
 */
interface SpeedSource {
    fun readings(): Flow<SpeedReading>
}
