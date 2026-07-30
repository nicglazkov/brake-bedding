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

    // The error text tells the user what to do. It does not name the broken rule.
    val problem = when {
        stopsValue == null || stopsValue <= 0 -> "Enter the number of stops for this stage"
        startValue == null || targetValue == null -> "Enter the two speeds"
        startValue <= targetValue -> "Enter a start speed that is more than the target speed"
        gapValue < 0 -> "Enter a distance of zero or more"
        else -> null
    }

    Text(
        text = if (existing == null) "New bedding stage" else "Edit the bedding stage",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(16.dp))

    NumberField(stops, { stops = it }, "Number of stops", keyboardType = KeyboardType.Number)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField(start, { start = it }, "Start speed (${units.speedLabel})", modifier = Modifier.weight(1f))
        NumberField(target, { target = it }, "Target speed (${units.speedLabel})", modifier = Modifier.weight(1f))
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
        Text(if (existing == null) "Add the stage" else "Save the changes")
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
        value == null || value <= 0 -> "Enter the distance for the cooldown"
        else -> null
    }

    Text(
        text = if (existing == null) "New cooldown" else "Edit the cooldown",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Drive this distance with minimum braking. This lets the pads and the " +
            "rotors become cool. A stop on hot brakes causes unwanted pad material on " +
            "the rotor surface.",
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
        Text(if (existing == null) "Add the cooldown" else "Save the changes")
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
        // Some locales have a comma on the number keyboard. The app uses the comma as
        // a decimal point. Then those users can type fractions.
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
 * Removes the zeros at the end. Then a field shows "42", not "42.0".
 *
 * The format uses [Locale.ROOT], not the locale of the user. This value goes into an
 * input field, and `toDoubleOrNull` reads it again. A locale with a decimal comma
 * would make a field that the app cannot parse. Then the save would not operate.
 */
private fun Double.round(decimals: Int = 0): String {
    val text = String.format(Locale.ROOT, "%.${decimals}f", this)
    return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
}
