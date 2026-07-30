package com.glazkov.brakebedding.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Makes the one DataStore of the app. On Android, the file must stay at the path that
 * version 2.1 used. Then an update keeps the data of the user.
 */
expect fun createAppDataStore(): DataStore<Preferences>

/**
 * Reads the stages JSON from the storage of the old Android versions. The iOS app has
 * no old versions, so its actual returns null.
 */
expect fun legacyStagesJson(): String?

/** The one DataStore instance. The repositories share it. */
object Stores {
    val dataStore: DataStore<Preferences> by lazy { createAppDataStore() }
}
