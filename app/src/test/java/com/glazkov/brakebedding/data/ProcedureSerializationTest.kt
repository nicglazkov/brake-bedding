package com.glazkov.brakebedding.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcedureSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * The defect in the first version: the write of a cooldown stage was correct, but
     * the read was not, because the two operations used different code. Now one
     * generated serializer does the write and the read. The defect is not possible.
     */
    @Test
    fun `a procedure containing a cooldown round-trips unchanged`() {
        val original = Procedure(
            name = "round trip",
            stages = listOf(
                BeddingStage(
                    numberOfStops = 20,
                    startSpeedMps = Units.mphToMps(42.0),
                    targetSpeedMps = Units.mphToMps(18.0),
                    gapDistanceMeters = Units.milesToMeters(0.3),
                    brakingIntensity = BrakingIntensity.LIGHT,
                ),
                CooldownStage(distanceMeters = Units.milesToMeters(6.0)),
            ),
        )

        val restored = json.decodeFromString<Procedure>(json.encodeToString(original))

        assertEquals(original, restored)
        assertTrue("the cooldown must still be a cooldown", restored.stages[1] is CooldownStage)
        assertEquals(6.0, Units.metersToMiles((restored.stages[1] as CooldownStage).distanceMeters), 0.0001)
    }

    @Test
    fun `every preset round-trips`() {
        Presets.all.forEach { preset ->
            assertEquals(preset, json.decodeFromString<Procedure>(json.encodeToString(preset)))
        }
    }

    @Test
    fun `a bedding stage rejects a target speed above its start speed`() {
        val error = runCatching {
            BeddingStage(
                numberOfStops = 5,
                startSpeedMps = Units.mphToMps(20.0),
                targetSpeedMps = Units.mphToMps(40.0),
                gapDistanceMeters = 0.0,
                brakingIntensity = BrakingIntensity.LIGHT,
            )
        }.exceptionOrNull()
        assertNotNull("an impossible stage should not be constructible", error)
    }

    // --- Legacy import ----------------------------------------------------------------

    @Test
    fun `legacy data imports with units converted`() {
        val legacy = """
            [{"type":"bedding","numberOfStops":20,"startSpeed":42.0,"targetSpeed":18.0,
              "gapDistance":0.3,"brakingIntensity":"LIGHT"},
             {"type":"cooldown","distance":6.0}]
        """.trimIndent()

        val imported = LegacyMigration.parse(legacy)
        assertNotNull(imported)
        assertEquals(2, imported!!.stages.size)

        val bedding = imported.stages[0] as BeddingStage
        assertEquals(20, bedding.numberOfStops)
        assertEquals(42.0, Units.mpsToMph(bedding.startSpeedMps), 0.001)
        assertEquals(18.0, Units.mpsToMph(bedding.targetSpeedMps), 0.001)
        assertEquals(0.3, Units.metersToMiles(bedding.gapDistanceMeters), 0.0001)
        assertEquals(BrakingIntensity.LIGHT, bedding.brakingIntensity)

        val cooldown = imported.stages[1] as CooldownStage
        assertEquals(6.0, Units.metersToMiles(cooldown.distanceMeters), 0.0001)
    }

    /** The oldest builds wrote stages with no type value. */
    @Test
    fun `legacy data without a type discriminator imports as bedding`() {
        val legacy = """
            [{"numberOfStops":10,"startSpeed":54.0,"targetSpeed":30.0,"gapDistance":0.62}]
        """.trimIndent()

        val imported = LegacyMigration.parse(legacy)
        assertNotNull(imported)
        val bedding = imported!!.stages.single() as BeddingStage
        assertEquals(10, bedding.numberOfStops)
        assertEquals(BrakingIntensity.MODERATE, bedding.brakingIntensity)
    }

    @Test
    fun `unreadable legacy entries are skipped rather than failing the import`() {
        val legacy = """
            [{"type":"bedding","numberOfStops":10,"startSpeed":54.0,"targetSpeed":30.0,"gapDistance":0.62},
             {"type":"bedding","numberOfStops":0,"startSpeed":10.0,"targetSpeed":50.0,"gapDistance":0.1},
             {"type":"cooldown","distance":6.0}]
        """.trimIndent()

        val imported = LegacyMigration.parse(legacy)
        assertNotNull(imported)
        assertEquals("the impossible middle stage should be dropped", 2, imported!!.stages.size)
    }

    @Test
    fun `garbage legacy data imports as nothing rather than crashing`() {
        assertNull(LegacyMigration.parse("not json at all"))
        assertNull(LegacyMigration.parse("[]"))
        assertNull(LegacyMigration.parse("""{"unexpected":"shape"}"""))
    }
}
