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

private val sections = listOf(
    GuideSection(
        "What bedding does",
        "Bedding lays an even film of pad material onto the rotor face and heat-cycles " +
            "the pad compound so it stops outgassing under load. Done properly it is why " +
            "new brakes stop quietly, bite consistently and resist fade. Skipped, it is " +
            "why they judder, squeal, or leave uneven deposits that feel like a warped rotor.",
    ),
    GuideSection(
        "Before you start",
        "Find a road you can use safely: straight, level, empty, with good sight lines and " +
            "somewhere to abort. A procedure needs several miles of continuous driving, and " +
            "you should not be stopping at junctions in the middle of it.\n\n" +
            "Mount the phone where you can see it without moving your head far. Turn on " +
            "spoken cues so you do not have to look at all.",
    ),
    GuideSection(
        "Reading the screen",
        "The whole screen is the instruction. Each phase has its own colour and its own " +
            "chevron, so you can tell them apart at a glance and without relying on colour.\n\n" +
            "Chevrons up means get to the target speed. Chevrons down means shed speed — " +
            "one for easing off, three for the stop itself. Two bars means hold what you " +
            "have while the bar at the bottom drains. A single bar means coast.",
    ),
    GuideSection(
        "During a stop",
        "Brake at the intensity the stage asks for and release at the target speed. Do not " +
            "come to a standstill, and do not hold the pedal once you are down to speed — " +
            "resting a hot pad against a stationary rotor is what prints an uneven deposit " +
            "into it.\n\nThen coast the gap. That gap is not padding; it is the time the " +
            "brakes need to shed heat before the next stop.",
    ),
    GuideSection(
        "The cooldown",
        "The final stage is several miles of ordinary driving with as little braking as you " +
            "can manage. Park at the end of it, not before. If you have to stop early, leave " +
            "the car in gear or on a wheel chock rather than holding it on the brake pedal.",
    ),
    GuideSection(
        "Safety",
        "Stay inside the speed limit and inside your own comfort. If a stage asks for a " +
            "speed the road does not allow, edit it down — the routine matters less than " +
            "not crashing.\n\nStop immediately if you smell burning, the pedal goes long or " +
            "soft, or the car pulls under braking. Fade is a sign to let everything cool, " +
            "not a sign to push on.\n\nThis app is a timer and a prompt. It cannot see the " +
            "road, and whatever the pad manufacturer printed in the box beats whatever is " +
            "on this screen.",
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
