package com.glazkov.brakebedding.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * The DataStore file of the Android app.
 *
 * The path is the exact path that version 2.1 used through the preferencesDataStore
 * delegate. Because of this, an update from 2.1 keeps the procedure and the settings
 * of the user.
 */
actual fun createAppDataStore(): DataStore<Preferences> {
    val context = PlatformContext.appContext
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            context.filesDir.resolve("datastore/brake_bedding.preferences_pb")
                .absolutePath.toPath()
        },
    )
}

/** Reads the stages JSON that versions before 2.0 kept in SharedPreferences. */
actual fun legacyStagesJson(): String? = runCatching {
    PlatformContext.appContext
        .getSharedPreferences("BrakeBeddingApp", android.content.Context.MODE_PRIVATE)
        .getString("stages", null)
}.getOrNull()
