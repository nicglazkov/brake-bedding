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
 * The speed from the GPS provider of the platform.
 *
 * This class uses [LocationManager], not the fused provider from Play Services. This
 * is intentional. The fused provider is better for position, but this app only wants
 * the ground speed that GPS reports directly. Also, the platform API keeps the app
 * free of components that are not open source.
 */
class LocationSpeedSource(private val context: Context) : SpeedSource {

    @SuppressLint("MissingPermission") // hasPermission() is checked before each use.
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

        // GPS reports approximately one time each second. A request for a zero
        // interval, as in the first version, gives no gain and uses more battery.
        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_UPDATE_INTERVAL_MS,
            0f,
            listener,
        )

        // Start with the last known data. Then a run that starts in a vehicle that
        // moves does not show the GPS message for more time than necessary.
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
     * True when the user gave access only to the approximate location. Speed data
     * comes in, but its source is a position with low accuracy. That is not
     * sufficient to hold a speed. The UI tells this to the driver.
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
