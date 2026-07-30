package com.glazkov.brakebedding.data

import com.glazkov.brakebedding.platform.randomUuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The brake force that the app tells the driver to use in a bedding stage. */
@Serializable
enum class BrakingIntensity(val displayName: String, val shortName: String) {
    LIGHT("Light braking", "Light"),
    MODERATE("Moderate braking", "Moderate"),
    FIRM("Firm braking", "Firm"),
    THRESHOLD("Threshold braking", "Threshold"),
    ABS("ABS braking", "ABS"),
}

/**
 * One step of a procedure.
 *
 * This is a sealed hierarchy with one generated serializer. Because of this, the
 * stored form and the parsed form always agree about the type of a stage. The first
 * implementation wrote stages with a manual Gson adapter, but read them as a
 * `List<BeddingStage>`. That changed each cooldown stage into a bedding stage with
 * all values at zero.
 */
@Serializable
sealed interface Stage {
    /** This value does not change when the user edits or moves stages. Lists use it as a key. */
    val id: String
}

@Serializable
@SerialName("bedding")
data class BeddingStage(
    override val id: String = newStageId(),
    val numberOfStops: Int,
    val startSpeedMps: Double,
    val targetSpeedMps: Double,
    val gapDistanceMeters: Double,
    val brakingIntensity: BrakingIntensity,
) : Stage {
    init {
        require(numberOfStops > 0) { "A bedding stage needs at least one stop" }
        require(startSpeedMps > targetSpeedMps) {
            "Start speed must be higher than target speed"
        }
        require(gapDistanceMeters >= 0) { "Gap distance cannot be negative" }
    }
}

@Serializable
@SerialName("cooldown")
data class CooldownStage(
    override val id: String = newStageId(),
    val distanceMeters: Double,
) : Stage {
    init {
        require(distanceMeters > 0) { "A cooldown stage needs a distance" }
    }
}

/** A full bedding procedure that the app can run. */
@Serializable
data class Procedure(
    val name: String = "My procedure",
    val stages: List<Stage> = emptyList(),
) {
    val isRunnable: Boolean get() = stages.isNotEmpty()

    val beddingStages: List<BeddingStage> get() = stages.filterIsInstance<BeddingStage>()

    /** The total number of stops. The progress bar uses this value. */
    val totalStops: Int get() = beddingStages.sumOf { it.numberOfStops }

    val hasCooldown: Boolean get() = stages.any { it is CooldownStage }

    /**
     * The approximate minimum distance for the procedure. The gap distances and the
     * cooldown are exact. The calculation does not include the distance for the
     * speed changes. Because of this, the real distance is always more.
     */
    val minimumDistanceMeters: Double
        get() = stages.sumOf { stage ->
            when (stage) {
                is BeddingStage -> stage.gapDistanceMeters * stage.numberOfStops
                is CooldownStage -> stage.distanceMeters
            }
        }
}

internal fun newStageId(): String = randomUuid()
