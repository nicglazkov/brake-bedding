package com.glazkov.brakebedding.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Presets
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.Stage
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.ui.EditorUiState
import com.glazkov.brakebedding.ui.theme.instrumentLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onUpsert: (Stage) -> Unit,
    onRemove: (String) -> Unit,
    onUndoRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onApplyPreset: (Procedure) -> Unit,
) {
    var editing by remember { mutableStateOf<StageEdit?>(null) }
    var presetsOpen by remember { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Procedure") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { presetsOpen = true }) { Text("Presets") }
                        DropdownMenu(
                            expanded = presetsOpen,
                            onDismissRequest = { presetsOpen = false },
                        ) {
                            Presets.all.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name) },
                                    onClick = {
                                        presetsOpen = false
                                        onApplyPreset(preset)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.procedure.name,
                    onValueChange = onRename,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.procedure.stages.isEmpty()) {
                item { EmptyState() }
            }

            itemsIndexed(state.procedure.stages, key = { _, stage -> stage.id }) { index, stage ->
                StageCard(
                    index = index,
                    stage = stage,
                    units = state.units,
                    canMoveUp = index > 0 && stage !is CooldownStage,
                    canMoveDown = index < state.procedure.stages.lastIndex &&
                        state.procedure.stages.getOrNull(index + 1) !is CooldownStage,
                    onEdit = {
                        editing = when (stage) {
                            is BeddingStage -> StageEdit.Bedding(stage)
                            is CooldownStage -> StageEdit.Cooldown(stage)
                        }
                    },
                    onRemove = {
                        onRemove(stage.id)
                        scope.launch {
                            val result = snackbars.showSnackbar(
                                message = "Stage removed",
                                actionLabel = "Undo",
                            )
                            if (result == SnackbarResult.ActionPerformed) onUndoRemove()
                        }
                    },
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { editing = StageEdit.Bedding(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stage")
                    }
                    OutlinedButton(
                        onClick = { editing = StageEdit.Cooldown(null) },
                        enabled = !state.procedure.hasCooldown,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cooldown")
                    }
                }
            }

            if (!state.procedure.hasCooldown && state.procedure.stages.isNotEmpty()) {
                item {
                    Text(
                        text = "This procedure has no cooldown stage. A cooldown is a " +
                            "necessary part of the procedure. Add a cooldown stage at the end.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    editing?.let { edit ->
        StageEditorSheet(
            edit = edit,
            units = state.units,
            onDismiss = { editing = null },
            onSave = { stage ->
                onUpsert(stage)
                editing = null
            },
        )
    }
}

@Composable
private fun StageCard(
    index: Int,
    stage: Stage,
    units: UnitSystem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when (stage) {
                    is BeddingStage -> "STAGE ${index + 1}"
                    is CooldownStage -> "COOLDOWN"
                },
                style = instrumentLabel,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stage.summary(units),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move the stage up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move the stage down")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit the stage")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove the stage")
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(modifier = Modifier.padding(vertical = 32.dp)) {
        Text(
            text = "This procedure has no stages",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Start from a preset, or add the stages that the pad manufacturer specifies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
