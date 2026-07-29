package com.glazkov.brakebedding.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glazkov.brakebedding.data.AppSettings
import com.glazkov.brakebedding.data.CooldownStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.ProcedureRepository
import com.glazkov.brakebedding.data.SettingsRepository
import com.glazkov.brakebedding.data.Stage
import com.glazkov.brakebedding.data.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val procedure: Procedure = Procedure(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
) {
    val units: UnitSystem get() = settings.unitSystem
}

/**
 * Edits the stored procedure.
 *
 * Every change writes straight through to storage rather than waiting for a Save button.
 * The previous version had one, and a commit titled "Fixed stages not saving" — removing
 * the button removes the whole category of bug, and deletions stay recoverable through
 * undo instead.
 */
class EditorViewModel(
    private val procedureRepository: ProcedureRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    /** The most recently removed stage and where it was, so it can be put back. */
    private var lastRemoved: Pair<Int, Stage>? = null

    init {
        viewModelScope.launch {
            procedureRepository.procedure.collect { procedure ->
                _state.update { it.copy(procedure = procedure, loaded = true) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun rename(name: String) = mutate { it.copy(name = name) }

    fun upsert(stage: Stage) = mutate { procedure ->
        val index = procedure.stages.indexOfFirst { it.id == stage.id }
        val stages = procedure.stages.toMutableList()
        if (index >= 0) stages[index] = stage else stages += stage
        procedure.copy(stages = orderCooldownLast(stages))
    }

    fun remove(id: String) = mutate { procedure ->
        val index = procedure.stages.indexOfFirst { it.id == id }
        if (index < 0) return@mutate procedure
        lastRemoved = index to procedure.stages[index]
        procedure.copy(stages = procedure.stages.filterNot { it.id == id })
    }

    fun undoRemove() {
        val (index, stage) = lastRemoved ?: return
        lastRemoved = null
        mutate { procedure ->
            val stages = procedure.stages.toMutableList()
            stages.add(index.coerceAtMost(stages.size), stage)
            procedure.copy(stages = orderCooldownLast(stages))
        }
    }

    fun move(from: Int, to: Int) = mutate { procedure ->
        if (from !in procedure.stages.indices || to !in procedure.stages.indices) {
            return@mutate procedure
        }
        val stages = procedure.stages.toMutableList()
        stages.add(to, stages.removeAt(from))
        procedure.copy(stages = orderCooldownLast(stages))
    }

    fun applyPreset(preset: Procedure) = mutate {
        // Fresh ids so the preset becomes the user's own copy rather than a shared one.
        preset.copy(
            stages = preset.stages.map { stage ->
                when (stage) {
                    is com.glazkov.brakebedding.data.BeddingStage ->
                        stage.copy(id = com.glazkov.brakebedding.data.newStageId())

                    is CooldownStage -> stage.copy(id = com.glazkov.brakebedding.data.newStageId())
                }
            },
        )
    }

    /**
     * A cooldown only makes sense once the heat has been put into the brakes, so it is
     * kept at the end no matter where it was dropped.
     */
    private fun orderCooldownLast(stages: List<Stage>): List<Stage> {
        val (cooldowns, bedding) = stages.partition { it is CooldownStage }
        return bedding + cooldowns
    }

    private fun mutate(block: (Procedure) -> Procedure) {
        val updated = block(_state.value.procedure)
        _state.update { it.copy(procedure = updated) }
        viewModelScope.launch { procedureRepository.save(updated) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                EditorViewModel(ProcedureRepository(app), SettingsRepository(app))
            }
        }
    }
}
