package com.glazkov.brakebedding.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.glazkov.brakebedding.engine.RunPhase

/**
 * A shape that identifies the phase without relying on its colour.
 *
 * Red-green is the pair the most common form of colour blindness collapses, and it is
 * also the pair carrying this app's two most consequential instructions. Chevron count
 * and direction encode the same information redundantly: up means go, down means shed
 * speed, and more chevrons means more urgency.
 */
@Composable
fun PhaseGlyph(
    phase: RunPhase,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = size.height * 0.09f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (phase) {
            RunPhase.SPEED_UP -> chevrons(count = 2, pointingUp = true, tint = tint, stroke = stroke)
            RunPhase.SLOW_DOWN -> chevrons(count = 1, pointingUp = false, tint = tint, stroke = stroke)
            RunPhase.BRAKE -> chevrons(count = 3, pointingUp = false, tint = tint, stroke = stroke)
            RunPhase.HOLD -> bars(count = 2, tint = tint, stroke = stroke)
            RunPhase.GAP -> bars(count = 1, tint = tint, stroke = stroke)
            RunPhase.COOLDOWN -> wave(tint = tint, stroke = stroke)
            RunPhase.FINISHED -> check(tint = tint, stroke = stroke)
            RunPhase.IDLE -> Unit
        }
    }
}

private fun DrawScope.chevrons(count: Int, pointingUp: Boolean, tint: Color, stroke: Stroke) {
    val width = size.width
    val chevronHeight = size.height / (count + 1.4f)
    val spacing = chevronHeight * 0.78f
    val totalHeight = chevronHeight + spacing * (count - 1)
    val startY = (size.height - totalHeight) / 2f

    repeat(count) { index ->
        val top = startY + spacing * index
        val path = Path().apply {
            if (pointingUp) {
                moveTo(0f, top + chevronHeight)
                lineTo(width / 2f, top)
                lineTo(width, top + chevronHeight)
            } else {
                moveTo(0f, top)
                lineTo(width / 2f, top + chevronHeight)
                lineTo(width, top)
            }
        }
        // Leading chevrons fade slightly so the set reads as a direction of travel.
        val fade = if (count == 1) 1f else 0.55f + 0.45f * (index / (count - 1f))
        drawPath(path, color = tint.copy(alpha = tint.alpha * fade), style = stroke)
    }
}

private fun DrawScope.bars(count: Int, tint: Color, stroke: Stroke) {
    val spacing = size.height * 0.22f
    val totalHeight = spacing * (count - 1)
    val startY = (size.height - totalHeight) / 2f
    repeat(count) { index ->
        val y = startY + spacing * index
        drawLine(
            color = tint,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.wave(tint: Color, stroke: Stroke) {
    val midY = size.height / 2f
    val amplitude = size.height * 0.16f
    val path = Path().apply {
        moveTo(0f, midY)
        cubicTo(
            size.width * 0.25f, midY - amplitude * 2,
            size.width * 0.25f, midY + amplitude * 2,
            size.width * 0.5f, midY,
        )
        cubicTo(
            size.width * 0.75f, midY - amplitude * 2,
            size.width * 0.75f, midY + amplitude * 2,
            size.width, midY,
        )
    }
    drawPath(path, color = tint, style = stroke)
}

private fun DrawScope.check(tint: Color, stroke: Stroke) {
    val box = Size(size.width, size.height)
    val path = Path().apply {
        moveTo(box.width * 0.12f, box.height * 0.52f)
        lineTo(box.width * 0.40f, box.height * 0.78f)
        lineTo(box.width * 0.88f, box.height * 0.22f)
    }
    drawPath(path, color = tint, style = stroke)
}
