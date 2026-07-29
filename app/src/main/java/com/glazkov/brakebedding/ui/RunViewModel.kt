package com.glazkov.brakebedding.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glazkov.brakebedding.data.AppSettings
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.Procedure
import com.glazkov.brakebedding.data.ProcedureRepository
import com.glazkov.brakebedding.data.SettingsRepository
import com.glazkov.brakebedding.engine.RunState
import com.glazkov.brakebedding.service.RunController
import com.glazkov.brakebedding.service.SignalStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RunUiState(
    val procedure: Procedure = Procedure(),
    val settings: AppSettings = AppSettings(),
    val run: RunState = RunState(),
    val speedMps: Double = 0.0,
    val signal: SignalStatus = SignalStatus.ACQUIRING,
) {
    val currentStage get() = procedure.stages.getOrNull(run.stageIndex)
    val currentBeddingStage get() = currentStage as? BeddingStage
}

/**
 * A window onto [RunController] for the run screen.
 *
 * The run itself does not live here — a ViewModel dies with its activity, and a bedding
 * run must not. This class only feeds the controller the stored procedure and settings,
 * and republishes the controller's state in the shape the screen consumes.
 */
class RunViewModel(
    private val procedureRepository: ProcedureRepository,
    settingsRepository: SettingsRepository,
    private val controller: RunController,
) : ViewModel() {

    val state: StateFlow<RunUiState> = controller.state
        .map { snapshot ->
            RunUiState(
                procedure = snapshot.procedure,
                settings = snapshot.settings,
                run = snapshot.run,
                speedMps = snapshot.speedMps,
                signal = snapshot.signal,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RunUiState())

    init {
        viewModelScope.launch {
            procedureRepository.migrateLegacyDataIfNeeded()
        }
        viewModelScope.launch {
            procedureRepository.procedure.collect(controller::setProcedure)
        }
        viewModelScope.launch {
            settingsRepository.settings.collect(controller::setSettings)
        }
    }

    fun start() = controller.start()

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    fun skipStage() = controller.skipStage()

    fun stop() = controller.stop()

    fun refreshPermissionState() = controller.refreshPermissionState()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                RunViewModel(
                    procedureRepository = ProcedureRepository(app),
                    settingsRepository = SettingsRepository(app),
                    controller = RunController.get(app),
                )
            }
        }
    }
}
