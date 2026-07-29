package com.glazkov.brakebedding.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Speed from the platform's GPS provider.
 *
 * This deliberately uses [LocationManager] rather than Play Services' fused provider.
 * The fused provider is better at blending sensors for position, but this app only wants
 * the Doppler-derived ground speed that GPS reports directly, and staying on the platform
 * API keeps the app free of proprietary dependencies.
 */
class LocationSpeedSource(private val context: Context) : SpeedSource {

    @SuppressLint("MissingPermission") // Guarded by hasPermission() before every use.
    override fun readings(): Flow<SpeedReading> = callbackFlow {
        val manager = ContextCompat.getSystemService(context, LocationManager::class.java)
        if (manager == null || !hasPermission()) {
            trySend(SpeedReading.ProviderDisabled)
            awaitClose { }
            return@callbackFlow
        }

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            trySend(SpeedReading.ProviderDisabled)
        } else {
            trySend(SpeedReading.Acquiring)
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toReading())
            }

            override fun onProviderEnabled(provider: String) {
                trySend(SpeedReading.Acquiring)
            }

            override fun onProviderDisabled(provider: String) {
                trySend(SpeedReading.ProviderDisabled)
            }
        }

        // GPS reports about once a second. Asking for zero, as the previous version did,
        // buys nothing and costs battery on a screen-on drive that runs for half an hour.
        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_UPDATE_INTERVAL_MS,
            0f,
            listener,
        )

        // Seed with the last known fix so a run started in a moving car does not sit on
        // "waiting for GPS" for a second longer than it has to.
        runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }
            .getOrNull()
            ?.takeIf { it.isRecent() }
            ?.let { trySend(it.toReading()) }

        awaitClose { manager.removeUpdates(listener) }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * True when the user granted only approximate location. Speed still arrives, but it
     * is derived from a coarse position and is not good enough to hold a speed against,
     * so the UI says so rather than quietly misleading the driver.
     */
    fun hasOnlyCoarsePermission(): Boolean =
        !hasPermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isGpsEnabled(): Boolean =
        ContextCompat.getSystemService(context, LocationManager::class.java)
            ?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

    private fun Location.toReading(): SpeedReading =
        if (hasSpeed()) {
            SpeedReading.Fix(
                speedMps = speed.toDouble(),
                accuracyMps = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
                atElapsedRealtimeMillis = elapsedRealtimeNanos / 1_000_000,
            )
        } else {
            SpeedReading.Acquiring
        }

    private fun Location.isRecent(): Boolean =
        SystemClock.elapsedRealtime() - (elapsedRealtimeNanos / 1_000_000) < LAST_KNOWN_MAX_AGE_MS

    private companion object {
        const val MIN_UPDATE_INTERVAL_MS = 500L
        const val LAST_KNOWN_MAX_AGE_MS = 5_000L
    }
}
