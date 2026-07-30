package com.glazkov.brakebedding.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * The iOS vibration cues, from the feedback generators. The brake cue is three
 * pulses. In a vehicle that moves, one pulse can feel the same as the road. A rhythm
 * cannot.
 */
actual class Haptics {

    private val impact = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notification = UINotificationFeedbackGenerator()

    actual fun phaseChange() {
        impact.impactOccurred()
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun brakeNow() {
        heavy.impactOccurred()
        pulseAfter(0.17) { heavy.impactOccurred() }
        pulseAfter(0.34) { heavy.impactOccurred() }
    }

    actual fun complete() {
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun pulseAfter(seconds: Double, block: () -> Unit) {
        val time = dispatch_time(DISPATCH_TIME_NOW, (seconds * 1_000_000_000).toLong())
        dispatch_after(time, dispatch_get_main_queue()) { block() }
    }
}
