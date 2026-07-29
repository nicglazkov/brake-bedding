package com.glazkov.brakebedding.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.engine.BeddingEngine
import com.glazkov.brakebedding.engine.RunPhase
import com.glazkov.brakebedding.service.SignalStatus
import com.glazkov.brakebedding.ui.RunUiState
import com.glazkov.brakebedding.ui.components.DrainBar
import com.glazkov.brakebedding.ui.components.PhaseGlyph
import com.glazkov.brakebedding.ui.components.StopLadder
import com.glazkov.brakebedding.ui.theme.PhasePalette
import com.glazkov.brakebedding.ui.theme.instrumentLabel
import com.glazkov.brakebedding.ui.theme.instrumentReadout
import com.glazkov.brakebedding.ui.theme.instrumentTelemetry
import com.glazkov.brakebedding.ui.theme.instrumentVerb

/**
 * The screen a driver looks at.
 *
 * It deliberately leaves Material 3's surface-and-card idiom behind: there is no app bar,
 * no elevation and no container, because the entire window is the signal. Everything
 * else in the app stays inside Material, which is the right idiom for a form you fill in
 * while parked.
 */
@Composable
fun RunScreen(
    state: RunUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkipStage: () -> Unit,
    onEditProcedure: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onRequestPermission: () -> Unit,
    onRefreshPermissionState: () -> Unit,
) {
    // Location can be granted, or GPS switched on, from system settings while the app is
    // in the background, so the state is re-read on every resume rather than only after
    // the in-app permission dialog.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefreshPermissionState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val palette = PhasePalette.of(state.run.phase)
    val field by animateColorAsState(
        targetValue = if (state.run.phase == RunPhase.IDLE) {
            MaterialTheme.colorScheme.background
        } else {
            palette.field
        },
        animationSpec = tween(durationMillis = 350),
        label = "field",
    )
    val onField = if (state.run.phase == RunPhase.IDLE) {
        MaterialTheme.colorScheme.onBackground
    } else {
        palette.onField
    }

    // System bar icons have to follow the phase colour, or they vanish against amber.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = onField.luminance() < 0.5f
                isAppearanceLightNavigationBars = onField.luminance() < 0.5f
            }
        }
    }

    // The screen must stay awake through a procedure that can run half an hour with no
    // touch input at all. Written in a SideEffect because composition itself must not
    // mutate the view; the flag also clears when the run ends, so a finished procedure
    // does not pin the screen on in a parked car.
    val keepAwake = state.settings.keepScreenOn && state.run.phase.isRunning
    SideEffect {
        if (view.keepScreenOn != keepAwake) view.keepScreenOn = keepAwake
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(field),
    ) {
        when (state.run.phase) {
            RunPhase.IDLE -> ReadyView(
                state = state,
                onStart = onStart,
                onEditProcedure = onEditProcedure,
                onOpenSettings = onOpenSettings,
                onOpenHelp = onOpenHelp,
                onRequestPermission = onRequestPermission,
            )

            RunPhase.FINISHED -> FinishedView(
                state = state,
                onField = onField,
                onDone = onStop,
            )

            else -> InstrumentView(
                state = state,
                onField = onField,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onSkipStage = onSkipStage,
            )
        }
    }
}

// --- The running instrument ---------------------------------------------------------

@Composable
private fun InstrumentView(
    state: RunUiState,
    onField: Color,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkipStage: () -> Unit,
) {
    val units = state.settings.unitSystem
    val stage = state.currentStage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        StopLadder(
            procedure = state.procedure,
            stageIndex = state.run.stageIndex,
            cycleIndex = state.run.cycleIndex,
            tint = onField,
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = eyebrowFor(state),
            style = instrumentLabel,
            color = onField.copy(alpha = 0.75f),
        )

        if (!state.signal.isUsable) {
            Spacer(Modifier.height(10.dp))
            SignalNotice(state.signal, onField)
        }

        // The centre stack is sized against the height it actually gets: in landscape the
        // full-size glyph, verb and readout add up to more than the viewport, and a plain
        // Column hands the shortfall to its last child — which silently squeezed the
        // target readout, the one number the instruction refers to, to zero height.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val compact = maxHeight < 340.dp
            // Worst case is landscape with the GPS notice showing: roughly 160dp for
            // this stack, so the compact metrics are sized to fit that with the label
            // still visible rather than tuned to the roomy portrait case.
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PhaseGlyph(
                    phase = state.run.phase,
                    tint = onField,
                    modifier = if (compact) {
                        Modifier.size(width = 52.dp, height = 34.dp)
                    } else {
                        Modifier.size(width = 108.dp, height = 78.dp)
                    },
                )
                Spacer(Modifier.height(if (compact) 6.dp else 18.dp))
                Text(
                    text = verbFor(state.run.phase),
                    style = if (compact) {
                        instrumentVerb.copy(fontSize = 34.sp, lineHeight = 36.sp)
                    } else {
                        instrumentVerb
                    },
                    color = onField,
                )
                readoutFor(state, units)?.let { (value, label) ->
                    Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                    Text(
                        text = value,
                        style = if (compact) {
                            instrumentReadout.copy(fontSize = 52.sp, lineHeight = 52.sp)
                        } else {
                            instrumentReadout
                        },
                        color = onField,
                    )
                    Text(
                        text = label,
                        style = instrumentLabel,
                        color = onField.copy(alpha = 0.75f),
                    )
                }
            }
        }

        drainFractionFor(state)?.let { fraction ->
            DrainBar(
                fraction = fraction,
                tint = onField,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(18.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Telemetry(
                value = units.formatSpeed(state.speedMps),
                label = "now ${units.speedLabel}",
                onField = onField,
            )
            (stage as? BeddingStage)?.let {
                Telemetry(
                    value = "${state.run.cycleIndex + 1}/${it.numberOfStops}",
                    label = "stop",
                    onField = onField,
                    alignEnd = true,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FieldButton(
                text = if (state.run.isPaused) "Resume" else "Pause",
                onField = onField,
                modifier = Modifier.weight(1f),
                onClick = if (state.run.isPaused) onResume else onPause,
            )
            FieldButton(
                text = "Skip stage",
                onField = onField,
                modifier = Modifier.weight(1f),
                onClick = onSkipStage,
            )
            FieldButton(
                text = "Stop",
                onField = onField,
                modifier = Modifier.weight(1f),
                onClick = onStop,
            )
        }
    }
}

@Composable
private fun Telemetry(
    value: String,
    label: String,
    onField: Color,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(text = value, style = instrumentTelemetry, color = onField)
        Text(
            text = label.uppercase(),
            style = instrumentLabel,
            color = onField.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun FieldButton(
    text: String,
    onField: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(width = 1.5.dp, brush = SolidColor(onField.copy(alpha = 0.5f))),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = onField),
        // The default button padding eats enough width in a three-up row to clip
        // "Skip stage" down to "Skip", which reads as a different, riskier action.
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(text, maxLines = 1, fontSize = 14.sp)
    }
}

@Composable
private fun SignalNotice(signal: SignalStatus, onField: Color) {
    val message = when (signal) {
        SignalStatus.ACQUIRING -> "Waiting for GPS"
        SignalStatus.LOST -> "GPS signal lost — the run is paused until it returns"
        SignalStatus.GPS_OFF -> "Location is switched off"
        SignalStatus.NO_PERMISSION -> "Location access is needed to read your speed"
        SignalStatus.COARSE_ONLY -> "Precise location is needed to read your speed"
        SignalStatus.OK -> return
    }
    Text(
        text = message,
        style = instrumentLabel,
        color = onField,
        modifier = Modifier
            .fillMaxWidth()
            .background(onField.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// --- Idle and finished ---------------------------------------------------------------

@Composable
private fun ReadyView(
    state: RunUiState,
    onStart: () -> Unit,
    onEditProcedure: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val units = state.settings.unitSystem
    val scheme = MaterialTheme.colorScheme
    val needsPermission = state.signal == SignalStatus.NO_PERMISSION ||
        state.signal == SignalStatus.COARSE_ONLY

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text("BRAKE BEDDING", style = instrumentLabel, color = scheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.procedure.name,
            style = MaterialTheme.typography.displaySmall,
            color = scheme.onBackground,
        )
        Spacer(Modifier.height(20.dp))

        StopLadder(
            procedure = state.procedure,
            stageIndex = 0,
            cycleIndex = -1,
            tint = scheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
        )
        Spacer(Modifier.height(20.dp))

        state.procedure.stages.forEachIndexed { index, stage ->
            Text(
                text = "${index + 1}  ${stage.summary(units)}",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "At least ${units.formatDistanceWithUnit(state.procedure.minimumDistanceMeters)} " +
                "of road, plus room to get up to speed.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        if (needsPermission) {
            Text(
                text = "Brake Bedding reads your speed from GPS. It needs precise " +
                    "location while the app is open, and nothing else.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Grant location access", fontSize = 18.sp)
            }
        } else {
            Button(
                onClick = onStart,
                enabled = state.procedure.isRunnable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Start", fontSize = 22.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onEditProcedure, modifier = Modifier.weight(1f)) {
                Text("Edit procedure")
            }
            TextButton(onClick = onOpenHelp, modifier = Modifier.weight(1f)) {
                Text("Guide")
            }
            TextButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text("Settings")
            }
        }
    }
}

@Composable
private fun FinishedView(
    state: RunUiState,
    onField: Color,
    onDone: () -> Unit,
) {
    val units = state.settings.unitSystem
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PhaseGlyph(
            phase = RunPhase.FINISHED,
            tint = onField,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text("BEDDED", style = instrumentVerb, color = onField)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "${state.run.completedStops} stops · " +
                units.formatDistanceWithUnit(state.run.distanceTraveledMeters),
            style = instrumentTelemetry,
            color = onField,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Keep rolling for a few more minutes if you can, and avoid holding " +
                "the brakes at a standstill until the rotors have cooled.",
            style = MaterialTheme.typography.bodyLarge,
            color = onField.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        FieldButton(
            text = "Done",
            onField = onField,
            onClick = onDone,
            modifier = Modifier.width(200.dp),
        )
    }
}

// --- Copy and derived values ---------------------------------------------------------

private fun verbFor(phase: RunPhase): String = when (phase) {
    RunPhase.SPEED_UP -> "SPEED UP"
    RunPhase.SLOW_DOWN -> "SLOW DOWN"
    RunPhase.HOLD -> "HOLD"
    RunPhase.BRAKE -> "BRAKE"
    RunPhase.GAP -> "COAST"
    RunPhase.COOLDOWN -> "COOL DOWN"
    RunPhase.FINISHED -> "BEDDED"
    RunPhase.IDLE -> ""
}

/** The number under the verb, and what it means. */
private fun readoutFor(state: RunUiState, units: UnitSystem): Pair<String, String>? {
    val stage = state.currentStage
    return when (state.run.phase) {
        RunPhase.SPEED_UP, RunPhase.SLOW_DOWN, RunPhase.HOLD ->
            (stage as? BeddingStage)?.let {
                units.formatSpeed(it.startSpeedMps) to "target ${units.speedLabel}"
            }

        RunPhase.BRAKE -> (stage as? BeddingStage)?.let {
            units.formatSpeed(it.targetSpeedMps) to "down to ${units.speedLabel}"
        }

        RunPhase.GAP, RunPhase.COOLDOWN ->
            units.formatDistance(state.run.remainingMeters) to "${units.distanceLabel} to go"

        else -> null
    }
}

private fun drainFractionFor(state: RunUiState): Float? {
    val stage = state.currentStage
    return when (state.run.phase) {
        RunPhase.HOLD ->
            (state.run.holdSecondsRemaining / BeddingEngine.HOLD_SECONDS).toFloat()

        RunPhase.GAP -> (stage as? BeddingStage)
            ?.takeIf { it.gapDistanceMeters > 0 }
            ?.let { (state.run.remainingMeters / it.gapDistanceMeters).toFloat() }

        RunPhase.COOLDOWN -> (stage as? CooldownStage)
            ?.let { (state.run.remainingMeters / it.distanceMeters).toFloat() }

        else -> null
    }
}

private fun eyebrowFor(state: RunUiState): String {
    val parts = mutableListOf(
        "STAGE ${state.run.stageIndex + 1} OF ${state.procedure.stages.size}",
    )
    (state.currentStage as? BeddingStage)?.let {
        parts += it.brakingIntensity.shortName.uppercase()
    }
    if (state.run.isPaused) parts += "PAUSED"
    return parts.joinToString("  ·  ")
}

internal fun com.glazkov.brakebedding.data.Stage.summary(units: UnitSystem): String = when (this) {
    is BeddingStage -> buildString {
        append("$numberOfStops stops, ")
        append("${units.formatSpeed(startSpeedMps)} to ${units.formatSpeed(targetSpeedMps)} ${units.speedLabel}")
        if (gapDistanceMeters > 0) {
            append(", ${units.formatDistanceWithUnit(gapDistanceMeters)} between")
        }
        append(" · ${brakingIntensity.shortName}")
    }

    is CooldownStage -> "Cooldown, ${units.formatDistanceWithUnit(distanceMeters)} without heavy braking"
}
