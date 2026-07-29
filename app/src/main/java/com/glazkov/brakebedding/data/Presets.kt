package com.glazkov.brakebedding.data

import com.glazkov.brakebedding.data.Units.milesToMeters
import com.glazkov.brakebedding.data.Units.mphToMps

/**
 * Starting points so a new user does not face an empty screen.
 *
 * These are commonly circulated street and track routines, not manufacturer
 * instructions. Whatever came in the box with the pads always wins, which is why the
 * UI presents these as editable starting points rather than as recommendations.
 */
object Presets {

    /** The routine most street pad makers describe some variation of. */
    val street = Procedure(
        name = "Street pads",
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

    /** Adds a hotter third stage for more aggressive pad compounds. */
    val performance = Procedure(
        name = "Performance pads",
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
            BeddingStage(
                numberOfStops = 10,
                startSpeedMps = mphToMps(72.0),
                targetSpeedMps = mphToMps(30.0),
                gapDistanceMeters = milesToMeters(0.15),
                brakingIntensity = BrakingIntensity.THRESHOLD,
            ),
            CooldownStage(distanceMeters = milesToMeters(6.0)),
        ),
    )

    /** A short routine for swapping pads onto already bedded rotors. */
    val quick = Procedure(
        name = "Quick re-bed",
        stages = listOf(
            BeddingStage(
                numberOfStops = 8,
                startSpeedMps = mphToMps(40.0),
                targetSpeedMps = mphToMps(15.0),
                gapDistanceMeters = milesToMeters(0.25),
                brakingIntensity = BrakingIntensity.MODERATE,
            ),
            CooldownStage(distanceMeters = milesToMeters(3.0)),
        ),
    )

    val all = listOf(street, performance, quick)

    val default = street
}
