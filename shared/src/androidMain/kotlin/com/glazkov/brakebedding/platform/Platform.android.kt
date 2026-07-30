package com.glazkov.brakebedding.platform

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import java.util.Locale
import java.util.UUID

/**
 * The application context for the actuals in this source set. The Application class
 * of the app sets it one time, before all other use.
 */
@SuppressLint("StaticFieldLeak") // Only the application context goes in here.
object PlatformContext {
    lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

actual fun monotonicMillis(): Long = SystemClock.elapsedRealtime()

actual fun randomUuid(): String = UUID.randomUUID().toString()

actual fun localizedDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)

actual fun appVersionName(): String {
    val context = PlatformContext.appContext
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }
}
