package com.glazkov.brakebedding.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Speaks the next instruction so the driver does not have to look at the phone.
 *
 * This is the feature that makes the app usable as intended: at 70 mph, glancing at a
 * screen to find out whether it is time to brake is exactly the thing the app should be
 * removing. Cues are short on purpose — a sentence is too long to hear out before the
 * moment it describes has passed.
 */
class VoiceCoach(context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.apply {
                    language = Locale.getDefault().takeIf {
                        isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
                    } ?: Locale.US
                    // Tagging cues as navigation guidance is what makes car head units and
                    // Bluetooth stacks duck the music rather than talk over it, the same
                    // treatment turn-by-turn directions get.
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setSpeechRate(SPEECH_RATE)
                }
                ready = true
            } else {
                Log.w(TAG, "Text to speech unavailable, cues will be silent")
            }
        }
    }

    /**
     * Says [text], dropping anything still queued.
     *
     * Flushing rather than queueing is deliberate: a stale instruction is worse than
     * silence, because the driver will act on whatever they hear last.
     */
    fun say(text: String) {
        if (!ready) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun stop() {
        engine?.stop()
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        const val TAG = "VoiceCoach"
        const val SPEECH_RATE = 1.1f
    }
}
