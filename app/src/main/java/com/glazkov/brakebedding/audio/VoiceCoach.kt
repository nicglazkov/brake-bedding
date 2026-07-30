package com.glazkov.brakebedding.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Speaks each instruction. Then it is not necessary for the driver to look at the
 * phone.
 *
 * This function makes the app safe for its purpose. At 70 mph, a look at a screen is
 * the thing that the app must remove. The cues are short. This is intentional. A
 * long sentence ends after the moment that it specifies.
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
                    // The cues have the navigation-guidance audio type. Because of
                    // this, vehicle audio units decrease the music volume for a cue.
                    // Navigation instructions get the same audio treatment.
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
     * Speaks [text] and discards the cues that are in the queue.
     *
     * The discard is intentional. An old instruction is worse than silence. The
     * driver obeys the last instruction that they hear.
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
