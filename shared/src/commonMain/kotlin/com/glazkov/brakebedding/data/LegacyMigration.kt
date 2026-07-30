package com.glazkov.brakebedding.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads procedures from old app versions. Those versions kept a Gson list in
 * SharedPreferences.
 *
 * The old format kept speeds in mph and distances in miles. It also set a "type"
 * value that the old versions wrote but did not read. This parser is manual, and it
 * must not change. It specifies a format that is not in the code at an other
 * location. Do not connect it to the current model.
 */
object LegacyMigration {

    const val LEGACY_PREFS_NAME = "BrakeBeddingApp"
    const val LEGACY_STAGES_KEY = "stages"

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Converts old JSON data into a [Procedure]. The parser discards each entry that
     * it cannot read. It returns null if no entry is usable. Then the caller can use
     * a preset.
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
        // Entries from before the cooldown function have no "type" value.
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

    /** Old data kept the enum name, the display name, or no value. */
    private fun parseIntensity(raw: String?): BrakingIntensity {
        if (raw == null) return BrakingIntensity.MODERATE
        BrakingIntensity.entries.firstOrNull { it.name == raw }?.let { return it }
        return BrakingIntensity.entries.firstOrNull { it.displayName == raw } ?: BrakingIntensity.MODERATE
    }
}
