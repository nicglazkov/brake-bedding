package com.glazkov.brakebedding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.glazkov.brakebedding.location.iosSpeedSource
import platform.UIKit.UIApplication

@Composable
actual fun rememberPlatformActions(onPermissionResult: () -> Unit): PlatformActions {
    // Core Location reports each authorization change; the listener refreshes the
    // signal state in the controller.
    DisposableEffect(Unit) {
        iosSpeedSource.authorizationListener = onPermissionResult
        onDispose { iosSpeedSource.authorizationListener = null }
    }

    return remember {
        object : PlatformActions {
            override fun requestLocationPermission() {
                iosSpeedSource.requestPermission()
            }

            /** iOS has no gate before a run. Live Activities use no permission dialog. */
            override fun startRun(start: () -> Unit) = start()
        }
    }
}

/**
 * iOS draws no icons over the app content; the status bar style follows the
 * interface style. No work is necessary here.
 */
@Composable
actual fun PlatformSystemBars(darkIcons: Boolean) = Unit

@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    SideEffect {
        UIApplication.sharedApplication.idleTimerDisabled = enabled
    }
}
