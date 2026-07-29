package com.glazkov.brakebedding.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/** How hard the driver is asked to press the pedal during a bedding stage. */
@Serializable
enum class BrakingIntensity(val displayName: String, val shortName: String) {
    LIGHT("Light braking", "Light"),
    MODERATE("Moderate braking", "Moderate"),
    FIRM("Firm braking", "Firm"),
    THRESHOLD("Hard, just short of ABS", "Threshold"),
    ABS("Hard enough to trigger ABS", "ABS"),
}

/**
 * One step of a procedure.
 *
 * This is a sealed hierarchy with a single generated polymorphic serializer, so the
 * stored form and the parsed form can never disagree about what a stage is. The previous
 * implementation wrote stages through a hand-written Gson adapter but read them back
 * through a plain `List<BeddingStage>` binding, which silently turned every cooldown
 * stage into an all-zero bedding stage.
 */
@Serializable
sealed interface Stage {
    /** Stable across edits and reorders so list UIs can key on it. */
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

/** A complete, runnable bedding procedure. */
@Serializable
data class Procedure(
    val name: String = "My procedure",
    val stages: List<Stage> = emptyList(),
) {
    val isRunnable: Boolean get() = stages.isNotEmpty()

    val beddingStages: List<BeddingStage> get() = stages.filterIsInstance<BeddingStage>()

    /** Total number of individual stops, used for the run progress indicator. */
    val totalStops: Int get() = beddingStages.sumOf { it.numberOfStops }

    val hasCooldown: Boolean get() = stages.any { it is CooldownStage }

    /**
     * Rough lower bound on how far the driver needs to travel. Gap distances and the
     * cooldown are exact; the accelerate/brake portion of each stop is not modelled, so
     * the real distance is always somewhat higher.
     */
    val minimumDistanceMeters: Double
        get() = stages.sumOf { stage ->
            when (stage) {
                is BeddingStage -> stage.gapDistanceMeters * stage.numberOfStops
                is CooldownStage -> stage.distanceMeters
            }
        }
}

internal fun newStageId(): String = UUID.randomUUID().toString()
