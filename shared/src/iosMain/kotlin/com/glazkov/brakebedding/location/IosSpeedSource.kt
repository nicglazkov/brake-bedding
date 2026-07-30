package com.glazkov.brakebedding.location

import com.glazkov.brakebedding.platform.monotonicMillis
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * The speed from Core Location.
 *
 * The manager runs with the navigation accuracy. Then the speed values come from
 * GPS, and the app can hold a speed against them. When a run is active,
 * [setBackgroundUpdates] keeps the data flow alive while the app is not on the
 * screen. The blue system indicator shows this to the user.
 */
class IosSpeedSource : SpeedSource {

    private val manager = CLLocationManager()
    private val latest = MutableStateFlow<SpeedReading>(SpeedReading.Acquiring)

    // The delegate of CLLocationManager is a weak reference. This property keeps the
    // object alive.
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            latest.value = location.toReading()
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            // A temporary failure. The staleness monitor in the controller shows the
            // correct signal state; no work is necessary here.
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (hasPermission()) {
                startUpdates()
            }
            authorizationListener?.invoke()
        }
    }

    /** The UI registers this to read the permission state again after a change. */
    var authorizationListener: (() -> Unit)? = null

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        manager.pausesLocationUpdatesAutomatically = false
        if (hasPermission()) startUpdates()
    }

    private fun startUpdates() {
        manager.startUpdatingLocation()
    }

    /**
     * Turns the background location mode on or off. On is applicable only during a
     * run; a permanent background flow would use battery without a cause.
     */
    fun setBackgroundUpdates(enabled: Boolean) {
        if (!hasPermission()) return
        manager.allowsBackgroundLocationUpdates = enabled
        manager.showsBackgroundLocationIndicator = enabled
    }

    override fun readings(): Flow<SpeedReading> = latest.asStateFlow()

    override fun hasPermission(): Boolean {
        val status: CLAuthorizationStatus = manager.authorizationStatus
        val authorized = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
        val accurate = manager.accuracyAuthorization ==
            CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
        return authorized && accurate
    }

    override fun hasOnlyCoarsePermission(): Boolean {
        val status: CLAuthorizationStatus = manager.authorizationStatus
        val authorized = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
        return authorized && manager.accuracyAuthorization ==
            CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy
    }

    override fun isLocationEnabled(): Boolean = CLLocationManager.locationServicesEnabled()

    override fun requestPermission() {
        manager.requestWhenInUseAuthorization()
    }

    private fun CLLocation.toReading(): SpeedReading {
        // A negative speed shows that the value is not valid.
        if (speed < 0) return SpeedReading.Acquiring
        return SpeedReading.Fix(
            speedMps = speed,
            accuracyMps = if (speedAccuracy >= 0) speedAccuracy else null,
            atMonotonicMillis = monotonicMillis(),
        )
    }
}

/** The one speed source of the iOS app. The session support also uses it. */
val iosSpeedSource: IosSpeedSource by lazy { IosSpeedSource() }

actual fun createSpeedSource(): SpeedSource = iosSpeedSource
