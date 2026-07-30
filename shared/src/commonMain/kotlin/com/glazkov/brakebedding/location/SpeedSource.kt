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
     * [atMonotonicMillis] comes from the monotonic clock, not from the wall clock.
     * The wall clock can move back when the network corrects the time. Then new data
     * would look old, and the run would stop during the procedure.
     */
    data class Fix(
        val speedMps: Double,
        val accuracyMps: Double?,
        val atMonotonicMillis: Long,
    ) : SpeedReading
}

/**
 * The source of the speed.
 *
 * This is an interface. Because of this, tests can supply their own samples, and each
 * platform supplies its own location system. The engine does not see a difference.
 */
interface SpeedSource {
    fun readings(): Flow<SpeedReading>

    /** True when the app has access to the accurate location. */
    fun hasPermission(): Boolean

    /** True when the app has access only to the approximate location. */
    fun hasOnlyCoarsePermission(): Boolean

    /** True when the location function of the device is on. */
    fun isLocationEnabled(): Boolean

    /** Shows the system dialog that asks for access to the location. */
    fun requestPermission()
}

/** Makes the speed source of the platform. */
expect fun createSpeedSource(): SpeedSource
