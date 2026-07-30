package com.glazkov.brakebedding.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Vibration cues that you can feel through a phone mount.
 *
 * The brake cue is three pulses, not one long vibration. In a vehicle that moves,
 * one long vibration can feel the same as the road. A rhythm cannot.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.getSystemService(context, VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ContextCompat.getSystemService(context, Vibrator::class.java)
    }

    fun phaseChange() = play(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))

    fun brakeNow() = play(
        VibrationEffect.createWaveform(longArrayOf(0, 90, 80, 90, 80, 90), -1),
    )

    fun complete() = play(
        VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 400), -1),
    )

    /**
     * A cue must not cause an app stop. The app operates in a vehicle that moves. An
     * app stop because the vibrator is not available is worse than no vibration.
     * Some device builds and work profiles refuse the permission.
     */
    private fun play(effect: VibrationEffect) {
        try {
            vibrator?.takeIf { it.hasVibrator() }?.vibrate(effect)
        } catch (e: SecurityException) {
            Log.w(TAG, "Vibration unavailable, continuing without haptic cues", e)
        }
    }

    private companion object {
        const val TAG = "Haptics"
    }
}
