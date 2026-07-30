package com.glazkov.brakebedding.ui

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
import com.glazkov.brakebedding.platform.Stores
import com.glazkov.brakebedding.service.Run
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
 * The run is not here. A ViewModel stops with its screen, and a bedding run must
 * not stop. This class only supplies the stored procedure and the settings to the
 * controller. It also converts the controller state into the shape for the screen.
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

    fun requestLocationPermission() = controller.requestLocationPermission()

    fun refreshPermissionState() = controller.refreshPermissionState()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RunViewModel(
                    procedureRepository = ProcedureRepository(Stores.dataStore),
                    settingsRepository = SettingsRepository(Stores.dataStore),
                    controller = Run.controller,
                )
            }
        }
    }
}
