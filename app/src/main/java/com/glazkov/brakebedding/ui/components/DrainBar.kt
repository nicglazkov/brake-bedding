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
 * One progress bar for each type of delay in the app.
 *
 * In a hold, the bar becomes empty in three seconds. In a gap or a cooldown, the bar
 * becomes empty with the distance. The two conditions have the same shape. Because of
 * this, the driver learns one signal: the bar becomes empty, then a change comes.
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
