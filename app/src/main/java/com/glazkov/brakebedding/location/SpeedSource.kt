package com.glazkov.brakebedding.location

import kotlinx.coroutines.flow.Flow

/** The speed data that the location system supplies. */
sealed interface SpeedReading {

    /** The app listens, but no usable data came in until now. */
    data object Acquiring : SpeedReading

    /** The location function is off. No data will come. */
    data object ProviderDisabled : SpeedReading

    /**
     * One speed measurement.
     *
     * [atElapsedRealtimeMillis] comes from the monotonic clock, not from the wall
     * clock. The wall clock can move back when the network corrects the time. Then
     * new data would look old, and the run would stop during the procedure.
     */
    data class Fix(
        val speedMps: Double,
        val accuracyMps: Float?,
        val atElapsedRealtimeMillis: Long,
    ) : SpeedReading
}

/**
 * The source of the speed.
 *
 * This is an interface. Because of this, tests can supply their own samples, and the
 * emulator can supply its own location. The engine does not see a difference.
 */
interface SpeedSource {
    fun readings(): Flow<SpeedReading>
}
