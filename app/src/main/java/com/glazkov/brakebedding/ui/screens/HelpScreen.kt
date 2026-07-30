package com.glazkov.brakebedding.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glazkov.brakebedding.ui.theme.instrumentLabel

private data class GuideSection(val title: String, val body: String)

// All guide text obeys ASD-STE100: approved verb forms, active voice, commands for
// instructions, and a maximum of 20 words for each instruction sentence.
private val sections = listOf(
    GuideSection(
        "What brake bedding does",
        "Brake bedding puts a thin, equal layer of pad material on the rotor surface. " +
            "It also heats the pads in controlled cycles.\n\n" +
            "Correct bedding gives brakes that are quiet and consistent. " +
            "If you do not bed the brakes, they can make noise, cause vibration, or " +
            "put unwanted pad material on the rotors.",
    ),
    GuideSection(
        "Before you start",
        "Find a road where the procedure is safe. The road must be straight, level, and " +
            "empty, with a clear view. You must have some miles of road without " +
            "intersections.\n\n" +
            "Attach the phone in a position near your line of view. Set the spoken " +
            "instructions to on. Then it is not necessary to look at the screen.",
    ),
    GuideSection(
        "The screen",
        "The full screen shows one instruction. Each instruction has its own color and " +
            "its own symbol. You do not identify an instruction only by its color.\n\n" +
            "Chevrons that point up: increase speed to the target. One chevron that " +
            "points down: decrease speed. Three chevrons that point down: brake. Two " +
            "bars: hold your speed until the bar at the bottom is empty. One bar: " +
            "drive, and do not brake.",
    ),
    GuideSection(
        "Each stop",
        "Apply the brakes with the force that the stage specifies. Release the brakes " +
            "at the target speed. Do not stop the vehicle. Do not hold the brake pedal " +
            "after you are at the target speed. A hot pad on a rotor that does not turn " +
            "causes an unwanted layer of pad material.\n\n" +
            "Then drive the distance between the stops. This distance lets the brakes " +
            "become more cool before the subsequent stop.",
    ),
    GuideSection(
        "The cooldown",
        "The last stage is some miles of usual speed with minimum braking. Park only " +
            "after the cooldown is complete. If a stop is necessary before that, put " +
            "the transmission in park or use a wheel chock. Do not hold the vehicle " +
            "with the brake pedal.",
    ),
    GuideSection(
        "Safety",
        "Obey the speed limits. If a stage specifies a speed that is too high for the " +
            "road, decrease the speed in the stage editor.\n\n" +
            "Stop the procedure immediately if you get the smell of hot brakes, if the " +
            "pedal becomes soft, or if the vehicle pulls to one side. Let the brakes " +
            "become cool.\n\n" +
            "The app cannot see the road. You are responsible for the vehicle. The " +
            "instructions from the pad manufacturer have priority over this app.",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guide") },
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
                .padding(horizontal = 20.dp),
        ) {
            sections.forEachIndexed { index, section ->
                Text(
                    text = "%02d".format(index + 1),
                    style = instrumentLabel,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(section.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = section.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
