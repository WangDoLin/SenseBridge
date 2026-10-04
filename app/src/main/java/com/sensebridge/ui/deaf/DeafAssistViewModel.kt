package com.sensebridge.ui.deaf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.monitoring.MonitoringCoordinator
import com.sensebridge.data.repository.UserPreferencesRepository
import com.sensebridge.input.sound.DecibelEnergyGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeafAssistViewModel @Inject constructor(
    private val monitoringCoordinator: MonitoringCoordinator,
    private val energyGate: DecibelEnergyGate,
    private val eventEngine: EventEngine,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isListening: StateFlow<Boolean> = monitoringCoordinator.isRecording
    val currentDecibels: StateFlow<Double> = monitoringCoordinator.currentDecibels
    val isBackgroundEnabled: StateFlow<Boolean> = monitoringCoordinator.isBackgroundEnabled

    // Filter only audio events in the recent events list
    val recentAudioEvents: StateFlow<List<SenseEvent>> = eventEngine.recentEvents
        .map { list -> list.filter { it.source == SensorySource.AUDIO_CLASSIFIER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thresholdDb: Double
        get() = energyGate.thresholdDb

    fun startListening(context: Context? = null) {
        monitoringCoordinator.startMonitoring()
    }

    fun stopListening(context: Context? = null) {
        monitoringCoordinator.stopMonitoring()
    }

    fun toggleBackgroundMonitoring(context: Context? = null) {
        val willEnable = !isBackgroundEnabled.value
        monitoringCoordinator.setBackgroundMonitoring(willEnable)
    }

    fun setSensitivity(isHighSensitivity: Boolean) {
        // High sensitivity for quiet room (55 dB); Normal for street (68 dB)
        val newThreshold = if (isHighSensitivity) 55.0 else 68.0
        energyGate.thresholdDb = newThreshold
        viewModelScope.launch {
            preferencesRepository.updateThresholdDb(newThreshold)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // If background monitoring is disabled, stop listening; otherwise keep scanning!
        if (!monitoringCoordinator.isBackgroundEnabled.value) {
            monitoringCoordinator.stopMonitoring()
        }
    }
}
