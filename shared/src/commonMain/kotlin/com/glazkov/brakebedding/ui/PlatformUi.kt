package com.glazkov.brakebedding.ui

import androidx.compose.runtime.Composable

/**
 * The platform actions that the run screen starts.
 *
 * Android shows its permission dialogs through activity launchers, and asks for the
 * notification permission before the first run. iOS asks through CLLocationManager
 * and has no gate before a run. The common UI only calls these two functions.
 */
interface PlatformActions {
    /** Shows the system dialog that asks for access to the location. */
    fun requestLocationPermission()

    /** Does the platform work before a run, then calls [start]. */
    fun startRun(start: () -> Unit)
}

@Composable
expect fun rememberPlatformActions(onPermissionResult: () -> Unit): PlatformActions

/** Sets the style of the system bars so that their icons stay visible on the field color. */
@Composable
expect fun PlatformSystemBars(darkIcons: Boolean)

/** Keeps the screen on while [enabled] is true. */
@Composable
expect fun KeepScreenOn(enabled: Boolean)
