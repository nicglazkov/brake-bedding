package com.glazkov.brakebedding.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The app's single DataStore.
 *
 * It lives on its own because the delegate may only be created once per file per
 * process; both repositories read from this same instance rather than each declaring
 * their own, which would throw at runtime the first time the second one was touched.
 */
internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brake_bedding")
