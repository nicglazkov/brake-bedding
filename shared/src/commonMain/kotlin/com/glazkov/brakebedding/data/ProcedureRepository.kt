package com.glazkov.brakebedding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glazkov.brakebedding.platform.legacyStagesJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Keeps the one procedure that the user operates with.
 *
 * The app keeps one procedure, not a set of them. This is intentional. The user
 * changes the procedure in the editor. The presets are the start points.
 */
class ProcedureRepository(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    val procedure: Flow<Procedure> = dataStore.data.map { prefs ->
        prefs[PROCEDURE_KEY]?.let { stored ->
            try {
                json.decodeFromString<Procedure>(stored)
            } catch (e: Exception) {
                println("ProcedureRepository: the stored procedure is not readable, a preset is used")
                null
            }
        } ?: Presets.default
    }

    suspend fun save(procedure: Procedure) {
        dataStore.edit { prefs ->
            prefs[PROCEDURE_KEY] = json.encodeToString(procedure)
        }
    }

    suspend fun current(): Procedure = procedure.first()

    /**
     * Imports one time the procedure from the old version of the app.
     *
     * Only the Android platform can find old data, and only when the application ID
     * is the same as the ID of the old build. On iOS and in the other conditions,
     * [legacyStagesJson] returns null and the import does nothing.
     */
    suspend fun migrateLegacyDataIfNeeded() {
        val alreadyRun = dataStore.data.first()[MIGRATION_KEY] != null
        if (alreadyRun) return

        val imported = legacyStagesJson()?.let { LegacyMigration.parse(it) }
        dataStore.edit { prefs ->
            if (imported != null && prefs[PROCEDURE_KEY] == null) {
                prefs[PROCEDURE_KEY] = json.encodeToString(imported)
                println("ProcedureRepository: ${imported.stages.size} stage(s) imported from the old format")
            }
            prefs[MIGRATION_KEY] = MIGRATION_VERSION
        }
    }

    private companion object {
        const val MIGRATION_VERSION = "v1"
        val PROCEDURE_KEY = stringPreferencesKey("procedure_json")
        val MIGRATION_KEY = stringPreferencesKey("legacy_migration")
    }
}
