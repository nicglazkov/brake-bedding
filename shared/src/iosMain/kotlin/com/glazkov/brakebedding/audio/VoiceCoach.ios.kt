@file:OptIn(ExperimentalForeignApi::class)

package com.glazkov.brakebedding.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive

/**
 * The iOS voice, from AVSpeechSynthesizer.
 *
 * The audio session has the duck-others option. Because of this, music becomes
 * quieter for a cue, as for navigation instructions.
 */
actual class VoiceCoach {

    private val synthesizer = AVSpeechSynthesizer()
    private var sessionConfigured = false

    private fun configureSessionIfNecessary() {
        if (sessionConfigured) return
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            category = AVAudioSessionCategoryPlayback,
            withOptions = AVAudioSessionCategoryOptionDuckOthers or
                AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers,
            error = null,
        )
        session.setActive(true, null)
        sessionConfigured = true
    }

    actual fun say(text: String) {
        configureSessionIfNecessary()
        if (synthesizer.speaking) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text).apply {
            rate = AVSpeechUtteranceDefaultSpeechRate * 1.1f
        }
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        if (synthesizer.speaking) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
    }

    actual fun shutdown() {
        stop()
        if (sessionConfigured) {
            AVAudioSession.sharedInstance().setActive(false, null)
            sessionConfigured = false
        }
    }
}
