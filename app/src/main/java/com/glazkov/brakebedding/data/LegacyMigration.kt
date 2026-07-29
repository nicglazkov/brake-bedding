package com.glazkov.brakebedding.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads procedures written by versions of the app that stored a Gson-encoded list in
 * SharedPreferences.
 *
 * The legacy format kept speeds in mph and distances in miles, and tagged entries with a
 * "type" discriminator that later versions added but never read back. This parser is
 * intentionally hand-written and frozen: it describes a format that no longer exists
 * anywhere else in the codebase, so it must not be refactored to share code with the
 * current model.
 */
object LegacyMigration {

    const val LEGACY_PREFS_NAME = "BrakeBeddingApp"
    const val LEGACY_STAGES_KEY = "stages"

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Converts a legacy JSON payload into a [Procedure], skipping any entry that cannot
     * be read. Returns null when there is nothing salvageable, so callers can fall back
     * to a preset.
     */
    fun parse(legacyJson: String): Procedure? {
        val stages = try {
            val array = lenientJson.parseToJsonElement(legacyJson) as? JsonArray ?: return null
            array.mapNotNull { element -> parseStage(element.jsonObject.toStringMap()) }
        } catch (e: Exception) {
            return null
        }
        return if (stages.isEmpty()) null else Procedure(name = "Imported procedure", stages = stages)
    }

    private fun kotlinx.serialization.json.JsonObject.toStringMap(): Map<String, String> =
        mapValues { (_, value) -> value.jsonPrimitive.content }

    private fun parseStage(fields: Map<String, String>): Stage? {
        // Entries written before the cooldown feature existed carry no discriminator.
        val type = fields["type"] ?: "bedding"
        return try {
            when (type) {
                "cooldown" -> {
                    val miles = fields["distance"]?.toDoubleOrNull() ?: return null
                    if (miles <= 0) null else CooldownStage(distanceMeters = Units.milesToMeters(miles))
                }

                else -> {
                    val stops = fields["numberOfStops"]?.toDoubleOrNull()?.toInt() ?: return null
                    val startMph = fields["startSpeed"]?.toDoubleOrNull() ?: return null
                    val targetMph = fields["targetSpeed"]?.toDoubleOrNull() ?: return null
                    val gapMiles = fields["gapDistance"]?.toDoubleOrNull() ?: 0.0
                    if (stops <= 0 || startMph <= targetMph) return null
                    BeddingStage(
                        numberOfStops = stops,
                        startSpeedMps = Units.mphToMps(startMph),
                        targetSpeedMps = Units.mphToMps(targetMph),
                        gapDistanceMeters = Units.milesToMeters(gapMiles.coerceAtLeast(0.0)),
                        brakingIntensity = parseIntensity(fields["brakingIntensity"]),
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Legacy data stored either the enum name or, in the oldest builds, nothing at all. */
    private fun parseIntensity(raw: String?): BrakingIntensity {
        if (raw == null) return BrakingIntensity.MODERATE
        BrakingIntensity.entries.firstOrNull { it.name == raw }?.let { return it }
        return BrakingIntensity.entries.firstOrNull { it.displayName == raw } ?: BrakingIntensity.MODERATE
    }
}
