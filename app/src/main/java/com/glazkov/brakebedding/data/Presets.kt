package com.glazkov.brakebedding.data

import com.glazkov.brakebedding.data.Units.milesToMeters
import com.glazkov.brakebedding.data.Units.mphToMps

/**
 * Start points for a new user. Then the first screen is not empty.
 *
 * These are usual procedures from the community, not manufacturer instructions. The
 * instructions from the pad manufacturer have priority. Because of this, the UI
 * shows the presets as start points that the user can edit.
 */
object Presets {

    /** The usual procedure for street pads. */
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

    /** This procedure has a third stage with more heat, for performance pads. */
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

    /** A short procedure for new pads on rotors that had a bedding before. */
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
