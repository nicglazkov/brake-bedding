package com.glazkov.brakebedding.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Phase changes you can feel through a phone mount.
 *
 * The braking cue is a triple pulse rather than a longer buzz: in a moving car a single
 * long vibration is easy to mistake for road noise, while a rhythm is not.
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
     * A cue is never worth a crash. The app is held in a mount in a moving car, so
     * losing the screen because the vibrator was unavailable would be worse than losing
     * the buzz — some OEM builds and work profiles deny this even when it is declared.
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
