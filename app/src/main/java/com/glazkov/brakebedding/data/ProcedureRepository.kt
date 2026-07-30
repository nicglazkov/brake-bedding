package com.glazkov.brakebedding.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
class ProcedureRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    val procedure: Flow<Procedure> = context.dataStore.data.map { prefs ->
        prefs[PROCEDURE_KEY]?.let { stored ->
            try {
                json.decodeFromString<Procedure>(stored)
            } catch (e: Exception) {
                Log.w(TAG, "Stored procedure could not be read, falling back to preset", e)
                null
            }
        } ?: Presets.default
    }

    suspend fun save(procedure: Procedure) {
        context.dataStore.edit { prefs ->
            prefs[PROCEDURE_KEY] = json.encodeToString(procedure)
        }
    }

    suspend fun current(): Procedure = procedure.first()

    /**
     * Imports one time the procedure from the old version of the app.
     *
     * The import finds data only when the application ID is the same as the ID of the
     * old build. SharedPreferences stay in the sandbox of one application ID. A
     * different ID has no access to them. Version 2.0 changed the ID, because Play
     * does not accept com.example. Because of this, the import finds data only after
     * an update with the same ID. In the other conditions it finds nothing.
     */
    suspend fun migrateLegacyDataIfNeeded() {
        val alreadyRun = context.dataStore.data.first()[MIGRATION_KEY] != null
        if (alreadyRun) return

        val legacy = runCatching {
            context.getSharedPreferences(LegacyMigration.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LegacyMigration.LEGACY_STAGES_KEY, null)
        }.getOrNull()

        val imported = legacy?.let { LegacyMigration.parse(it) }
        context.dataStore.edit { prefs ->
            if (imported != null && prefs[PROCEDURE_KEY] == null) {
                prefs[PROCEDURE_KEY] = json.encodeToString(imported)
                Log.i(TAG, "Imported ${imported.stages.size} stage(s) from the legacy format")
            }
            prefs[MIGRATION_KEY] = MIGRATION_VERSION
        }
    }

    private companion object {
        const val TAG = "ProcedureRepository"
        const val MIGRATION_VERSION = "v1"
        val PROCEDURE_KEY = stringPreferencesKey("procedure_json")
        val MIGRATION_KEY = stringPreferencesKey("legacy_migration")
    }
}
