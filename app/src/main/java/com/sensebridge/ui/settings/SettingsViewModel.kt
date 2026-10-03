package com.sensebridge.ui.settings

import androidx.lifecycle.ViewModel
import com.sensebridge.core.dispatcher.OutputConfiguration
import com.sensebridge.core.dispatcher.SensoryDispatcher
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.input.sound.DecibelEnergyGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dispatcher: SensoryDispatcher,
    private val energyGate: DecibelEnergyGate,
    private val eventEngine: EventEngine
) : ViewModel() {

    val config: StateFlow<OutputConfiguration> = dispatcher.config

    private val _thresholdDb = MutableStateFlow(energyGate.thresholdDb)
    val thresholdDb: StateFlow<Double> = _thresholdDb.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        dispatcher.updateConfiguration(config.value.copy(enableHaptic = enabled))
    }

    fun setVoiceEnabled(enabled: Boolean) {
        dispatcher.updateConfiguration(config.value.copy(enableVoice = enabled))
    }

    fun setVisualEnabled(enabled: Boolean) {
        dispatcher.updateConfiguration(config.value.copy(enableVisual = enabled))
    }

    fun updateThreshold(newThreshold: Double) {
        energyGate.thresholdDb = newThreshold
        _thresholdDb.value = newThreshold
    }

    fun clearAllLogs() {
        eventEngine.clearHistory()
    }
}
