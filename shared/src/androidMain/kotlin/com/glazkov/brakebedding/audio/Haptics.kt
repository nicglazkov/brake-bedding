package com.glazkov.brakebedding.audio

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.glazkov.brakebedding.platform.PlatformContext

/** The Android vibration cues. */
actual class Haptics {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.getSystemService(PlatformContext.appContext, VibratorManager::class.java)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ContextCompat.getSystemService(PlatformContext.appContext, Vibrator::class.java)
    }

    actual fun phaseChange() =
        play(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))

    actual fun brakeNow() = play(
        VibrationEffect.createWaveform(longArrayOf(0, 90, 80, 90, 80, 90), -1),
    )

    actual fun complete() = play(
        VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 400), -1),
    )

    /**
     * A cue must not cause an app stop. The app operates in a vehicle that moves.
     * Some device builds and work profiles refuse the vibration permission.
     */
    private fun play(effect: VibrationEffect) {
        try {
            vibrator?.takeIf { it.hasVibrator() }?.vibrate(effect)
        } catch (e: SecurityException) {
            Log.w(TAG, "Vibration is not available; the app continues without it", e)
        }
    }

    private companion object {
        const val TAG = "Haptics"
    }
}
