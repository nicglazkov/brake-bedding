package com.glazkov.brakebedding.ui.theme

import androidx.compose.ui.graphics.Color
import com.glazkov.brakebedding.engine.RunPhase

// The brand colors: warm stone tones and an ember accent. Their source is the hot
// rotor in the launcher icon, not a default Material palette.
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
 * The color that a phase gives to the full run screen.
 *
 * These colors do not change with the wallpaper. Dynamic color is the usual default.
 * But red must be the brake color on each device, and a dynamic palette can supply
 * two phase colors that are almost the same. Clear signals are more important than
 * style.
 *
 * A color is never the only signal for a phase. Each phase also has its own symbol.
 * The usual type of color blindness makes red and green look the same, and those
 * two colors carry the two most important instructions.
 */
data class PhaseColors(val field: Color, val onField: Color)

object PhasePalette {
    private val speedUp = PhaseColors(Color(0xFF0B8A52), Color.White)
    private val slowDown = PhaseColors(Color(0xFFE8A317), Ink)
    private val hold = PhaseColors(Color(0xFF1668C4), Color.White)
    private val brake = PhaseColors(Color(0xFFD92D20), Color.White)

    /** The gap is the rest period. Its color is dark and does not use attention. */
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
