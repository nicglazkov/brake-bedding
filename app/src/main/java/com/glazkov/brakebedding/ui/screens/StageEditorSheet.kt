package com.glazkov.brakebedding.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.BrakingIntensity
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Stage
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.data.newStageId
import com.glazkov.brakebedding.ui.theme.instrumentLabel
import java.util.Locale

/** What the sheet is currently editing. */
sealed interface StageEdit {
    data class Bedding(val existing: BeddingStage?) : StageEdit
    data class Cooldown(val existing: CooldownStage?) : StageEdit
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StageEditorSheet(
    edit: StageEdit,
    units: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (Stage) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            when (edit) {
                is StageEdit.Bedding -> BeddingForm(edit.existing, units, onSave)
                is StageEdit.Cooldown -> CooldownForm(edit.existing, units, onSave)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeddingForm(
    existing: BeddingStage?,
    units: UnitSystem,
    onSave: (Stage) -> Unit,
) {
    var stops by remember { mutableStateOf(existing?.numberOfStops?.toString() ?: "") }
    var start by remember { mutableStateOf(existing?.let { units.speedFromMps(it.startSpeedMps).round() } ?: "") }
    var target by remember { mutableStateOf(existing?.let { units.speedFromMps(it.targetSpeedMps).round() } ?: "") }
    var gap by remember { mutableStateOf(existing?.let { units.distanceFromMeters(it.gapDistanceMeters).round(2) } ?: "") }
    var intensity by remember { mutableStateOf(existing?.brakingIntensity ?: BrakingIntensity.MODERATE) }

    val stopsValue = stops.toIntOrNull()
    val startValue = start.toDoubleOrNull()
    val targetValue = target.toDoubleOrNull()
    val gapValue = gap.toDoubleOrNull() ?: 0.0

    // The error text names the fix, not the rule that was broken.
    val problem = when {
        stopsValue == null || stopsValue <= 0 -> "Enter how many stops this stage should do"
        startValue == null || targetValue == null -> "Enter both speeds"
        startValue <= targetValue -> "Start speed has to be higher than the speed you brake down to"
        gapValue < 0 -> "Distance between stops cannot be negative"
        else -> null
    }

    Text(
        text = if (existing == null) "New bedding stage" else "Edit bedding stage",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(16.dp))

    NumberField(stops, { stops = it }, "Number of stops", keyboardType = KeyboardType.Number)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField(start, { start = it }, "From (${units.speedLabel})", modifier = Modifier.weight(1f))
        NumberField(target, { target = it }, "Down to (${units.speedLabel})", modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    NumberField(gap, { gap = it }, "Distance between stops (${units.distanceLabel})")

    Spacer(Modifier.height(20.dp))
    Text("BRAKING", style = instrumentLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrakingIntensity.entries.forEach { option ->
            FilterChip(
                selected = option == intensity,
                onClick = { intensity = option },
                label = { Text(option.shortName) },
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    problem?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
    }
    Button(
        onClick = {
            onSave(
                BeddingStage(
                    id = existing?.id ?: newStageId(),
                    numberOfStops = stopsValue!!,
                    startSpeedMps = units.speedToMps(startValue!!),
                    targetSpeedMps = units.speedToMps(targetValue!!),
                    gapDistanceMeters = units.distanceToMeters(gapValue),
                    brakingIntensity = intensity,
                ),
            )
        },
        enabled = problem == null,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(if (existing == null) "Add stage" else "Save changes")
    }
}

@Composable
private fun CooldownForm(
    existing: CooldownStage?,
    units: UnitSystem,
    onSave: (Stage) -> Unit,
) {
    var distance by remember {
        mutableStateOf(existing?.let { units.distanceFromMeters(it.distanceMeters).round(1) } ?: "")
    }
    val value = distance.toDoubleOrNull()
    val problem = when {
        value == null || value <= 0 -> "Enter how far to drive while the brakes cool"
        else -> null
    }

    Text(
        text = if (existing == null) "New cooldown" else "Edit cooldown",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Drive this far without heavy braking so the pads and rotors shed heat " +
            "evenly. Coming to a stop on hot brakes is what leaves pad deposits behind.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    NumberField(distance, { distance = it }, "Distance (${units.distanceLabel})")

    Spacer(Modifier.height(20.dp))
    problem?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
    }
    Button(
        onClick = {
            onSave(
                CooldownStage(
                    id = existing?.id ?: newStageId(),
                    distanceMeters = units.distanceToMeters(value!!),
                ),
            )
        },
        enabled = problem == null,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(if (existing == null) "Add cooldown" else "Save changes")
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        // Decimal-comma locales put a comma on the numeric keyboard; treating it as a
        // decimal point lets those users type fractions instead of silently losing the key.
        onValueChange = { text ->
            onValueChange(text.replace(',', '.').filter { it.isDigit() || it == '.' })
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Trims trailing zeroes so a field shows "42" rather than "42.0".
 *
 * Formatted with [Locale.ROOT] rather than the user's locale, because this value goes
 * straight into an editable field that is read back with `toDoubleOrNull`. A locale that
 * writes a decimal comma would produce a field the app could no longer parse, and the
 * stage would silently refuse to save.
 */
private fun Double.round(decimals: Int = 0): String {
    val text = String.format(Locale.ROOT, "%.${decimals}f", this)
    return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
}
