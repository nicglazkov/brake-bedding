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
 * Stores the single procedure the user is working with.
 *
 * The app deliberately keeps one active procedure rather than a library of them; the
 * editor is where a procedure is shaped and presets are the way to start over.
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
     * Imports a procedure saved by the pre-Compose version of the app, once.
     *
     * Only reaches anything when the application ID is unchanged from the build that
     * wrote it: SharedPreferences live inside the app sandbox, so a renamed package
     * cannot see the old app's data no matter what it does. Version 2.0 did rename the
     * package away from com.example, which Play rejects, so in practice this path fires
     * for same-ID sideloaded upgrades and quietly finds nothing otherwise.
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
