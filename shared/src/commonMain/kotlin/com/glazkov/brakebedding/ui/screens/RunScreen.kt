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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
import com.glazkov.brakebedding.ui.KeepScreenOn
import com.glazkov.brakebedding.ui.PlatformSystemBars
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
 * The screen for the driver.
 *
 * This screen does not use the usual Material 3 surfaces and cards. It has no app
 * bar, no elevation, and no container. The full window is the signal. The other
 * screens use Material. That is correct for forms that you use when the vehicle is
 * parked.
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
    // The user can give access to the location in the system settings while the app
    // is not on the screen. Because of this, the app reads the state again at each
    // resume, not only after the permission dialog.
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

    // The system bar icons must agree with the phase color. If they do not, they are
    // not visible on the amber field.
    PlatformSystemBars(darkIcons = onField.luminance() < 0.5f)

    // The screen must stay on through a procedure of possibly 30 minutes with no
    // touch input. The flag also goes off when the run ends. Then the screen of a
    // parked vehicle can go off.
    KeepScreenOn(enabled = state.settings.keepScreenOn && state.run.phase.isRunning)

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

        // The center stack measures the height that it gets. In landscape, the
        // full-size symbol, command, and readout are higher than the viewport. A
        // simple Column then decreases the height of its last child. That made the
        // target readout not visible.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val compact = maxHeight < 340.dp
            // The worst condition is landscape with the GPS message. Then this stack
            // gets approximately 160 dp. The compact sizes go into that height with
            // the label visible.
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
                label = "speed ${units.speedLabel}",
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
                text = if (state.run.isPaused) "Continue" else "Pause",
                onField = onField,
                modifier = Modifier.weight(1f),
                onClick = if (state.run.isPaused) onResume else onPause,
            )
            FieldButton(
                text = "Next stage",
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
        // The default button padding uses too much width in a row of three buttons.
        // The text "Next stage" then loses its second word and gets a different
        // meaning.
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(text, maxLines = 1, fontSize = 14.sp)
    }
}

@Composable
private fun SignalNotice(signal: SignalStatus, onField: Color) {
    val message = when (signal) {
        SignalStatus.ACQUIRING -> "There is no GPS signal yet"
        SignalStatus.LOST -> "The GPS signal is lost. The run will continue when the signal is available."
        SignalStatus.GPS_OFF -> "The location function of this device is off"
        SignalStatus.NO_PERMISSION -> "The app must have access to your location to read your speed"
        SignalStatus.COARSE_ONLY -> "The app must have access to your accurate location to read your speed"
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
            text = "You must have a minimum of " +
                "${units.formatDistanceWithUnit(state.procedure.minimumDistanceMeters)} of road, " +
                "and more distance to get to each start speed.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        if (needsPermission) {
            Text(
                text = "The app reads your speed from GPS. The app must have access to " +
                    "your accurate location when it is open. The app does not use your " +
                    "location for other functions.",
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
                Text("Give access to location", fontSize = 18.sp)
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
                Text("Edit the procedure")
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
        Text("COMPLETE", style = instrumentVerb, color = onField)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "${state.run.completedStops} stops · " +
                units.formatDistanceWithUnit(state.run.distanceTraveledMeters),
            style = instrumentTelemetry,
            color = onField,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Continue to drive for some minutes if it is possible. Do not hold " +
                "the brake pedal when the vehicle is stopped and the rotors are hot.",
            style = MaterialTheme.typography.bodyLarge,
            color = onField.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        FieldButton(
            text = "OK",
            onField = onField,
            onClick = onDone,
            modifier = Modifier.width(200.dp),
        )
    }
}

// --- Copy and derived values ---------------------------------------------------------

// The command labels obey ASD-STE100. "BRAKE" and "DRIVE" are technical verbs of the
// vehicle domain. "COOLDOWN" is a technical name in this application.
private fun verbFor(phase: RunPhase): String = when (phase) {
    RunPhase.SPEED_UP -> "INCREASE SPEED"
    RunPhase.SLOW_DOWN -> "DECREASE SPEED"
    RunPhase.HOLD -> "HOLD SPEED"
    RunPhase.BRAKE -> "BRAKE"
    RunPhase.GAP -> "DRIVE"
    RunPhase.COOLDOWN -> "COOLDOWN"
    RunPhase.FINISHED -> "COMPLETE"
    RunPhase.IDLE -> ""
}

/** The number below the command, and its data label. */
private fun readoutFor(state: RunUiState, units: UnitSystem): Pair<String, String>? {
    val stage = state.currentStage
    return when (state.run.phase) {
        RunPhase.SPEED_UP, RunPhase.SLOW_DOWN, RunPhase.HOLD ->
            (stage as? BeddingStage)?.let {
                units.formatSpeed(it.startSpeedMps) to "target ${units.speedLabel}"
            }

        RunPhase.BRAKE -> (stage as? BeddingStage)?.let {
            units.formatSpeed(it.targetSpeedMps) to "brake to ${units.speedLabel}"
        }

        RunPhase.GAP, RunPhase.COOLDOWN ->
            units.formatDistance(state.run.remainingMeters) to "${units.distanceLabel} more"

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
            append(", ${units.formatDistanceWithUnit(gapDistanceMeters)} between stops")
        }
        append(" · ${brakingIntensity.shortName}")
    }

    is CooldownStage -> "Cooldown, ${units.formatDistanceWithUnit(distanceMeters)} with minimum braking"
}
