package com.glazkov.brakebedding.ui.theme

import androidx.compose.ui.graphics.Color
import com.glazkov.brakebedding.engine.RunPhase

// Brand. Warm stone neutrals and an ember accent, taken from the rotor-at-temperature
// idea in the launcher icon rather than from a default Material palette.
val Ember = Color(0xFFC2410C)
val EmberDeep = Color(0xFF7A2E0B)
val EmberBright = Color(0xFFEA580C)
val Graphite = Color(0xFF16120F)
val GraphiteRaised = Color(0xFF231D19)
val Bone = Color(0xFFFBF8F6)
val BoneRaised = Color(0xFFFFFFFF)
val Steel = Color(0xFFA8A29E)
val Ink = Color(0xFF1C1917)

/**
 * The colour a phase paints the entire run screen.
 *
 * These are fixed rather than derived from the wallpaper. Dynamic colour is the modern
 * default, but a driver learning that red means brake should get the same red on every
 * device, and a Material You palette could easily hand back two phase colours that are
 * near neighbours. Identity here is worth less than being unmistakable.
 *
 * Colour never carries a phase on its own: [com.glazkov.brakebedding.ui.components]
 * pairs every one of these with a distinct chevron, because red and green are exactly
 * the pair that the most common form of colour blindness collapses.
 */
data class PhaseColors(val field: Color, val onField: Color)

object PhasePalette {
    private val speedUp = PhaseColors(Color(0xFF0B8A52), Color.White)
    private val slowDown = PhaseColors(Color(0xFFE8A317), Ink)
    private val hold = PhaseColors(Color(0xFF1668C4), Color.White)
    private val brake = PhaseColors(Color(0xFFD92D20), Color.White)

    /** The gap is the rest period, so it stays dark and quiet and spends no attention. */
    private val gap = PhaseColors(Color(0xFF2C3742), Color.White)
    private val cooldown = PhaseColors(Color(0xFF115E59), Color.White)
    private val finished = PhaseColors(Ember, Color.White)

    fun of(phase: RunPhase): PhaseColors = when (phase) {
        RunPhase.SPEED_UP -> speedUp
        RunPhase.SLOW_DOWN -> slowDown
        RunPhase.HOLD -> hold
        RunPhase.BRAKE -> brake
        RunPhase.GAP -> gap
        RunPhase.COOLDOWN -> cooldown
        RunPhase.FINISHED -> finished
        RunPhase.IDLE -> gap
    }
}
