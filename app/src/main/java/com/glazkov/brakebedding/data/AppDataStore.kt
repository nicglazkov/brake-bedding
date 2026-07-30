package com.glazkov.brakebedding.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The one DataStore of the app.
 *
 * This is in its own file because one process can make the delegate only one time.
 * The two repositories read from this one instance. Two separate delegates would
 * cause an exception at the first use of the second one.
 */
internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brake_bedding")
