package com.glazkov.brakebedding.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * One progress affordance, reused for every kind of waiting the app does.
 *
 * During a hold it drains over three seconds; during a gap or a cooldown it drains over
 * the distance left. Giving both the same shape means the driver learns a single thing —
 * "this empties, then something changes" — instead of two.
 */
@Composable
fun DrainBar(
    fraction: Float,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "drain",
    )

    Canvas(modifier = modifier) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = tint.copy(alpha = tint.alpha * 0.22f),
            size = size,
            cornerRadius = radius,
        )
        if (animated > 0f) {
            drawRoundRect(
                color = tint,
                size = Size(size.width * animated, size.height),
                cornerRadius = radius,
            )
        }
    }
}
