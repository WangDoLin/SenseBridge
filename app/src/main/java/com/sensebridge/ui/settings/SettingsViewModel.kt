package com.sensebridge.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.sensebridge.core.dispatcher.OutputConfiguration
import com.sensebridge.core.dispatcher.SensoryDispatcher
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.service.SenseBridgeForegroundService
import com.sensebridge.input.sound.AudioRecorderManager
import com.sensebridge.input.sound.DecibelEnergyGate
import com.sensebridge.output.haptic.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dispatcher: SensoryDispatcher,
    private val energyGate: DecibelEnergyGate,
    private val eventEngine: EventEngine,
    private val audioRecorderManager: AudioRecorderManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    val config: StateFlow<OutputConfiguration> = dispatcher.config
    val isBackgroundEnabled: StateFlow<Boolean> = audioRecorderManager.isBackgroundEnabled

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

    fun toggleBackgroundMonitoring(context: Context) {
        val willEnable = !isBackgroundEnabled.value
        audioRecorderManager.setBackgroundMonitoring(willEnable)
        if (willEnable) {
            SenseBridgeForegroundService.start(context)
            audioRecorderManager.startListening()
        } else {
            SenseBridgeForegroundService.stop(context)
            audioRecorderManager.stopListening()
        }
    }

    fun updateThreshold(newThreshold: Double) {
        energyGate.thresholdDb = newThreshold
        _thresholdDb.value = newThreshold
    }

    fun testHapticVibration() {
        hapticManager.trigger(PriorityLevel.CRITICAL_P0)
    }

    fun clearAllLogs() {
        eventEngine.clearHistory()
    }
}
