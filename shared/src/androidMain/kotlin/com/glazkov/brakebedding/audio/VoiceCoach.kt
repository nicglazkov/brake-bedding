package com.glazkov.brakebedding.audio

import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import com.glazkov.brakebedding.platform.PlatformContext
import java.util.Locale

/**
 * The Android voice, from the system TextToSpeech engine.
 *
 * The cues have the navigation-guidance audio type. Because of this, vehicle audio
 * units decrease the music volume for a cue. Navigation instructions get the same
 * audio treatment.
 */
actual class VoiceCoach {

    private var engine: TextToSpeech? = null
    private var ready = false

    init {
        engine = TextToSpeech(PlatformContext.appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.apply {
                    language = Locale.getDefault().takeIf {
                        isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
                    } ?: Locale.US
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
                Log.w(TAG, "Text to speech is not available; the cues stay silent")
            }
        }
    }

    actual fun say(text: String) {
        if (!ready) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    actual fun stop() {
        engine?.stop()
    }

    actual fun shutdown() {
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
