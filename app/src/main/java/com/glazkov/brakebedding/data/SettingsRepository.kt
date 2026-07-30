package com.glazkov.brakebedding.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user settings. They change the display of a run, not its content. */
data class AppSettings(
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val voiceCues: Boolean = true,
    val hapticCues: Boolean = true,
    val keepScreenOn: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            unitSystem = prefs[UNIT_KEY]
                ?.let { stored -> UnitSystem.entries.firstOrNull { it.name == stored } }
                ?: UnitSystem.IMPERIAL,
            voiceCues = prefs[VOICE_KEY] ?: true,
            hapticCues = prefs[HAPTIC_KEY] ?: true,
            keepScreenOn = prefs[SCREEN_KEY] ?: true,
        )
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) = edit { it[UNIT_KEY] = unitSystem.name }

    suspend fun setVoiceCues(enabled: Boolean) = edit { it[VOICE_KEY] = enabled }

    suspend fun setHapticCues(enabled: Boolean) = edit { it[HAPTIC_KEY] = enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) = edit { it[SCREEN_KEY] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val UNIT_KEY = stringPreferencesKey("unit_system")
        val VOICE_KEY = booleanPreferencesKey("voice_cues")
        val HAPTIC_KEY = booleanPreferencesKey("haptic_cues")
        val SCREEN_KEY = booleanPreferencesKey("keep_screen_on")
    }
}
