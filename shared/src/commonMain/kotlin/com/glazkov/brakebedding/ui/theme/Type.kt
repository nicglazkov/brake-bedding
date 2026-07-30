package com.glazkov.brakebedding.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Each live number in this app has a monospace font.
 *
 * The style agrees with vehicle instruments. But there is also a technical cause.
 * The speed display changes four times each second. In a proportional font, the
 * digits change their width with each value, and the number moves. Monospace digits
 * do not move. Because of this, the driver can read the speed quickly.
 */
val InstrumentNumerals = FontFamily.Monospace

/** Small monospace labels in capitals, as on the dials of an instrument cluster. */
val instrumentLabel = TextStyle(
    fontFamily = InstrumentNumerals,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.6.sp,
)

/** The command that the driver reads. Its size makes it legible at the edge of vision. */
val instrumentVerb = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Black,
    fontSize = 64.sp,
    lineHeight = 66.sp,
    letterSpacing = 1.sp,
    textAlign = TextAlign.Center,
)

/** The target of the command: a speed or a distance. */
val instrumentReadout = TextStyle(
    fontFamily = InstrumentNumerals,
    fontWeight = FontWeight.Black,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-2).sp,
    textAlign = TextAlign.Center,
)

/** The smaller values at the bottom of the run screen. */
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
        // The section headers in the editor use the same label style. Then the two
        // parts of the app look like one instrument.
        labelSmall = instrumentLabel,
    )
}
