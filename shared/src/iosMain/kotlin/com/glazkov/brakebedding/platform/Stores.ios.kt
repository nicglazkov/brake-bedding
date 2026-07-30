package com.glazkov.brakebedding.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** The DataStore file of the iOS app, in the Documents directory. */
@OptIn(ExperimentalForeignApi::class)
actual fun createAppDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val documents: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            val base = requireNotNull(documents?.path) { "The Documents directory is not available" }
            "$base/brake_bedding.preferences_pb".toPath()
        },
    )

/** iOS has no old versions with legacy data. */
actual fun legacyStagesJson(): String? = null
