package com.glazkov.brakebedding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import com.glazkov.brakebedding.platform.Stores
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glazkov.brakebedding.data.AppSettings
import com.glazkov.brakebedding.data.SettingsRepository
import com.glazkov.brakebedding.data.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setUnitSystem(system: UnitSystem) = update { repository.setUnitSystem(system) }

    fun setVoiceCues(enabled: Boolean) = update { repository.setVoiceCues(enabled) }

    fun setHapticCues(enabled: Boolean) = update { repository.setHapticCues(enabled) }

    fun setKeepScreenOn(enabled: Boolean) = update { repository.setKeepScreenOn(enabled) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(SettingsRepository(Stores.dataStore))
            }
        }
    }
}
