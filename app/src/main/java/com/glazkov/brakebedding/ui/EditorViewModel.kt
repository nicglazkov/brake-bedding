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
 * The app writes each change to storage immediately. There is no Save button. The
 * first version had one, and also a commit with the title "Fixed stages not saving".
 * Without the button, that type of defect is not possible. The Undo function keeps
 * removed stages recoverable.
 */
class EditorViewModel(
    private val procedureRepository: ProcedureRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    /** The last removed stage and its position. The Undo function uses this. */
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
        // New ids make the preset the user's own copy, not a shared object.
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
     * A cooldown is correct only after the brakes are hot. Because of this, the app
     * keeps the cooldown at the end, independent of its drop position.
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
