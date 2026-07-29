package com.glazkov.brakebedding.data

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Speeds and distances are stored in SI units everywhere inside the app and converted
 * only when they are shown to or entered by the user. Keeping one canonical unit means
 * the engine never has to know which unit the user prefers.
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
 * The unit system the user reads and writes values in. Procedures are portable between
 * systems because they are stored in SI regardless of which one is selected.
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

    /** Whole-number speed, which is all a driver can act on at a glance. */
    fun formatSpeed(mps: Double): String = speedFromMps(mps).roundToInt().toString()

    fun formatSpeedWithUnit(mps: Double): String = "${formatSpeed(mps)} $speedLabel"

    /**
     * Distances shrink towards zero during a run, so the precision grows as the number
     * gets small — "0.42 mi" is useful, "0 mi" for anything under half a mile is not.
     *
     * Formatted in the reader's locale, so a decimal comma appears where that is what the
     * driver expects. Values that have to be parsed back, such as the contents of an
     * editable field, must not use this.
     */
    fun formatDistance(meters: Double): String {
        val value = distanceFromMeters(meters)
        val locale = Locale.getDefault()
        return when {
            value >= 10 -> String.format(locale, "%.0f", value)
            value >= 1 -> String.format(locale, "%.1f", value)
            else -> String.format(locale, "%.2f", value)
        }
    }

    fun formatDistanceWithUnit(meters: Double): String =
        "${formatDistance(meters)} $distanceLabel"
}
