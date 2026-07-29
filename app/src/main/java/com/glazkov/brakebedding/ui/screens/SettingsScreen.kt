package com.glazkov.brakebedding.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glazkov.brakebedding.data.AppSettings
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.ui.theme.instrumentLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    appVersion: String,
    onBack: () -> Unit,
    onUnitSystem: (UnitSystem) -> Unit,
    onVoiceCues: (Boolean) -> Unit,
    onHapticCues: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionLabel("UNITS")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                UnitSystem.entries.forEachIndexed { index, system ->
                    SegmentedButton(
                        selected = settings.unitSystem == system,
                        onClick = { onUnitSystem(system) },
                        shape = SegmentedButtonDefaults.itemShape(index, UnitSystem.entries.size),
                    ) {
                        Text("${system.speedLabel} / ${system.distanceLabel}")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Procedures are stored independently of this, so switching units " +
                    "converts what you see rather than changing the routine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionLabel("DURING A RUN")
            SettingSwitch(
                title = "Spoken cues",
                description = "Calls out each instruction so you can keep your eyes on the road.",
                checked = settings.voiceCues,
                onCheckedChange = onVoiceCues,
            )
            SettingSwitch(
                title = "Vibration",
                description = "A pulse on every phase change, and a triple pulse to brake.",
                checked = settings.hapticCues,
                onCheckedChange = onHapticCues,
            )
            SettingSwitch(
                title = "Keep screen on",
                description = "Holds the display awake while a procedure is running.",
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOn,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionLabel("ABOUT")
            Text("Brake Bedding $appVersion", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Open source under Apache 2.0. Speed comes from your device's GPS " +
                    "and never leaves the phone; the app has no network access at all.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = instrumentLabel,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
