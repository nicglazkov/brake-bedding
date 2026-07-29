package com.glazkov.brakebedding.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Every live number in this app is monospaced.
 *
 * It reads as instrumentation, which is the right vernacular, but the reason it is not
 * only a style choice is that the speed readout updates four times a second. In a
 * proportional face the digits change width as they change value and the whole number
 * visibly shimmers; monospaced digits hold still, which is what makes a glance at speed
 * possible while driving.
 */
val InstrumentNumerals = FontFamily.Monospace

/** Small uppercase mono labels, the way a gauge cluster labels its dials. */
val instrumentLabel = TextStyle(
    fontFamily = InstrumentNumerals,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.6.sp,
)

/** The one word the driver actually reads, sized to be legible in peripheral vision. */
val instrumentVerb = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Black,
    fontSize = 64.sp,
    lineHeight = 66.sp,
    letterSpacing = 1.sp,
    textAlign = TextAlign.Center,
)

/** The target the verb refers to: a speed to reach, a distance to cover. */
val instrumentReadout = TextStyle(
    fontFamily = InstrumentNumerals,
    fontWeight = FontWeight.Black,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-2).sp,
    textAlign = TextAlign.Center,
)

/** The smaller supporting figures along the bottom of the run screen. */
val instrumentTelemetry = TextStyle(
    fontFamily = InstrumentNumerals,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.5).sp,
)

val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        // Section headers in the editor borrow the gauge-label treatment so the two
        // halves of the app read as the same instrument.
        labelSmall = instrumentLabel,
    )
}
