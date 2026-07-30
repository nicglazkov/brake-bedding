package com.glazkov.brakebedding.data

import com.glazkov.brakebedding.platform.localizedDecimal
import kotlin.math.roundToInt

/**
 * The app keeps all speeds and all distances in SI units. It converts the values only
 * for the display and for the user input. There is one internal unit. Because of
 * this, the selected unit system has no effect on the engine.
 */
object Units {
    const val METERS_PER_MILE = 1609.344
    const val METERS_PER_KILOMETER = 1000.0
    const val MPS_PER_MPH = 0.44704
    const val MPS_PER_KPH = 1.0 / 3.6

    fun mphToMps(mph: Double) = mph * MPS_PER_MPH
    fun mpsToMph(mps: Double) = mps / MPS_PER_MPH
    fun kphToMps(kph: Double) = kph * MPS_PER_KPH
    fun mpsToKph(mps: Double) = mps / MPS_PER_KPH

    fun milesToMeters(miles: Double) = miles * METERS_PER_MILE
    fun metersToMiles(meters: Double) = meters / METERS_PER_MILE
    fun kilometersToMeters(km: Double) = km * METERS_PER_KILOMETER
    fun metersToKilometers(meters: Double) = meters / METERS_PER_KILOMETER
}

/**
 * The unit system for the display and for the user input. A procedure operates with
 * each system, because the app keeps the values in SI units.
 */
enum class UnitSystem(
    val speedLabel: String,
    val distanceLabel: String,
) {
    IMPERIAL("mph", "mi"),
    METRIC("km/h", "km"),
    ;

    fun speedFromMps(mps: Double): Double = when (this) {
        IMPERIAL -> Units.mpsToMph(mps)
        METRIC -> Units.mpsToKph(mps)
    }

    fun speedToMps(value: Double): Double = when (this) {
        IMPERIAL -> Units.mphToMps(value)
        METRIC -> Units.kphToMps(value)
    }

    fun distanceFromMeters(meters: Double): Double = when (this) {
        IMPERIAL -> Units.metersToMiles(meters)
        METRIC -> Units.metersToKilometers(meters)
    }

    fun distanceToMeters(value: Double): Double = when (this) {
        IMPERIAL -> Units.milesToMeters(value)
        METRIC -> Units.kilometersToMeters(value)
    }

    /** The speed as a whole number. A driver cannot use more precision. */
    fun formatSpeed(mps: Double): String = speedFromMps(mps).roundToInt().toString()

    fun formatSpeedWithUnit(mps: Double): String = "${formatSpeed(mps)} $speedLabel"

    /**
     * A distance decreases to zero during a run. Because of this, the precision
     * increases when the number becomes small. "0.42 mi" is useful. "0 mi" for all
     * distances below a half mile is not useful.
     *
     * The format uses the locale of the user. Then a decimal comma shows where the
     * driver expects one. Do not use this function for values that the app parses
     * again, for example the content of an input field.
     */
    fun formatDistance(meters: Double): String {
        val value = distanceFromMeters(meters)
        return when {
            value >= 10 -> localizedDecimal(value, 0)
            value >= 1 -> localizedDecimal(value, 1)
            else -> localizedDecimal(value, 2)
        }
    }

    fun formatDistanceWithUnit(meters: Double): String =
        "${formatDistance(meters)} $distanceLabel"
}
