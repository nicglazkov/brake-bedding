package com.glazkov.brakebedding.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure

/**
 * The whole procedure as one strip: a tick per stop, grouped by stage.
 *
 * This answers the question a driver actually forms between stops — "how many more of
 * these?" — which a percentage or a "cycle 7 of 20" line does not, because the shape of
 * the remaining work is the useful part. It sits at the top edge rather than the centre
 * because it belongs to the unhurried glance during a coast, not to the braking moment.
 *
 * The original project shipped a custom view for something like this and then removed it
 * again, unused; this is that idea rebuilt around what the run actually needs to show.
 */
@Composable
fun StopLadder(
    procedure: Procedure,
    stageIndex: Int,
    cycleIndex: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    if (procedure.stages.isEmpty()) return

    // A cooldown is one long stretch rather than a count of stops, so it is given the
    // width of a few stops to read as the sustained thing it is.
    val weights = procedure.stages.map { stage ->
        when (stage) {
            is BeddingStage -> stage.numberOfStops
            is CooldownStage -> COOLDOWN_WEIGHT
        }
    }

    Canvas(modifier = modifier) {
        val stageGap = STAGE_GAP.toPx()
        val tickGap = TICK_GAP.toPx()
        val totalTicks = weights.sum()
        val totalStageGaps = stageGap * (procedure.stages.size - 1)
        val totalTickGaps = tickGap * (totalTicks - procedure.stages.size)
        val tickWidth = ((size.width - totalStageGaps - totalTickGaps) / totalTicks)
            .coerceAtLeast(1f)

        var x = 0f
        procedure.stages.forEachIndexed { index, stage ->
            val ticks = weights[index]
            val isCooldown = stage is CooldownStage

            repeat(ticks) { tick ->
                val done = index < stageIndex ||
                    (index == stageIndex && !isCooldown && tick < cycleIndex)
                val current = index == stageIndex &&
                    (isCooldown || tick == cycleIndex)

                val alpha = when {
                    current -> 1f
                    done -> 0.85f
                    else -> 0.3f
                }
                val height = if (current) size.height else size.height * 0.55f
                val top = (size.height - height) / 2f

                drawRoundRect(
                    color = tint.copy(alpha = tint.alpha * alpha),
                    topLeft = Offset(x, top),
                    size = Size(tickWidth, height),
                    cornerRadius = CornerRadius(tickWidth.coerceAtMost(height) / 2f),
                )
                x += tickWidth + if (tick < ticks - 1) tickGap else 0f
            }
            x += stageGap
        }
    }
}

private val STAGE_GAP = 10.dp
private val TICK_GAP = 2.dp
private const val COOLDOWN_WEIGHT = 4
