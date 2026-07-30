package com.glazkov.brakebedding.service

import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.engine.RunPhase

/**
 * The instruction texts for the run surfaces outside the app: the Android
 * notification and the iOS Live Activity. One common source keeps the two platforms
 * identical. The texts obey ASD-STE100.
 */
object RunTexts {

    fun title(snapshot: RunSnapshot): String {
        val units = snapshot.settings.unitSystem
        val phase = snapshot.run.phase
        return when {
            snapshot.run.isPaused -> "The run is on pause"
            phase == RunPhase.FINISHED -> "The procedure is complete"
            else -> instruction(snapshot, units)
        }
    }

    fun text(snapshot: RunSnapshot): String = when (snapshot.run.phase) {
        RunPhase.FINISHED ->
            "${snapshot.run.completedStops} stops · " +
                snapshot.settings.unitSystem.formatDistanceWithUnit(snapshot.run.distanceTraveledMeters)

        else -> {
            val stage = "Stage ${snapshot.run.stageIndex + 1} of ${snapshot.procedure.stages.size}"
            (snapshot.currentStage as? BeddingStage)
                ?.let { "$stage · stop ${snapshot.run.cycleIndex + 1} of ${it.numberOfStops}" }
                ?: stage
        }
    }

    private fun instruction(snapshot: RunSnapshot, units: UnitSystem): String {
        val stage = snapshot.currentStage
        return when (snapshot.run.phase) {
            RunPhase.SPEED_UP -> (stage as? BeddingStage)
                ?.let { "Increase speed to ${units.formatSpeedWithUnit(it.startSpeedMps)}" }
                ?: "Increase speed"

            RunPhase.SLOW_DOWN -> (stage as? BeddingStage)
                ?.let { "Decrease speed to ${units.formatSpeedWithUnit(it.startSpeedMps)}" }
                ?: "Decrease speed"

            RunPhase.HOLD -> (stage as? BeddingStage)
                ?.let { "Hold ${units.formatSpeedWithUnit(it.startSpeedMps)}" } ?: "Hold this speed"

            RunPhase.BRAKE -> (stage as? BeddingStage)
                ?.let { "Brake to ${units.formatSpeedWithUnit(it.targetSpeedMps)}" } ?: "Brake"

            RunPhase.GAP ->
                "Drive ${units.formatDistanceWithUnit(snapshot.run.remainingMeters)} more"

            RunPhase.COOLDOWN ->
                "Cooldown: drive ${units.formatDistanceWithUnit(snapshot.run.remainingMeters)} more"

            else -> "Brake bedding"
        }
    }
}
