package com.glazkov.brakebedding.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

@Composable
actual fun rememberPlatformActions(onPermissionResult: () -> Unit): PlatformActions {
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { onPermissionResult() }

    // On Android 13 and newer, the run notification is possible only with the
    // POST_NOTIFICATIONS permission. The app asks at the first Start. The run starts
    // with each answer. A refusal only hides the notification. The service operates.
    var pendingStart: (() -> Unit)? = remember { null }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        pendingStart?.invoke()
        pendingStart = null
    }

    return remember(context) {
        object : PlatformActions {
            override fun requestLocationPermission() {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }

            override fun startRun(start: () -> Unit) {
                val needsAsk = Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                if (needsAsk) {
                    pendingStart = start
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    start()
                }
            }
        }
    }
}

@Composable
actual fun PlatformSystemBars(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = darkIcons
                isAppearanceLightNavigationBars = darkIcons
            }
        }
    }
}

@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    SideEffect {
        if (view.keepScreenOn != enabled) view.keepScreenOn = enabled
    }
}
