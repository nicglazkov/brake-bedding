package com.glazkov.brakebedding.audio

/**
 * Speaks each instruction. Then it is not necessary for the driver to look at the
 * phone. The cues are short. This is intentional. A long sentence ends after the
 * moment that it specifies.
 */
expect class VoiceCoach() {
    /** Speaks [text] and discards the cues that are in the queue. */
    fun say(text: String)

    fun stop()

    fun shutdown()
}

/**
 * Vibration cues that you can feel through a phone mount. The brake cue is a rhythm,
 * not one long vibration. In a vehicle that moves, one long vibration can feel the
 * same as the road. A rhythm cannot.
 */
expect class Haptics() {
    fun phaseChange()

    fun brakeNow()

    fun complete()
}
