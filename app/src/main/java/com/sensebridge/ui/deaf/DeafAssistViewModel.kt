package com.sensebridge.ui.deaf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.service.SenseBridgeForegroundService
import com.sensebridge.input.sound.AudioRecorderManager
import com.sensebridge.input.sound.DecibelEnergyGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DeafAssistViewModel @Inject constructor(
    private val audioRecorderManager: AudioRecorderManager,
    private val energyGate: DecibelEnergyGate,
    private val eventEngine: EventEngine
) : ViewModel() {

    val isListening: StateFlow<Boolean> = audioRecorderManager.isRecording
    val currentDecibels: StateFlow<Double> = audioRecorderManager.currentDecibels
    val isBackgroundEnabled: StateFlow<Boolean> = audioRecorderManager.isBackgroundEnabled

    // Filter only audio events in the recent events list
    val recentAudioEvents: StateFlow<List<SenseEvent>> = eventEngine.recentEvents
        .map { list -> list.filter { it.source == SensorySource.AUDIO_CLASSIFIER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thresholdDb: Double
        get() = energyGate.thresholdDb

    fun startListening(context: Context? = null) {
        audioRecorderManager.startListening()
        context?.let { SenseBridgeForegroundService.start(it) }
    }

    fun stopListening(context: Context? = null) {
        audioRecorderManager.stopListening()
        context?.let { SenseBridgeForegroundService.stop(it) }
    }

    fun toggleBackgroundMonitoring(context: Context) {
        val willEnable = !isListening.value
        if (willEnable) {
            audioRecorderManager.setBackgroundMonitoring(true)
            SenseBridgeForegroundService.start(context)
            audioRecorderManager.startListening()
        } else {
            audioRecorderManager.setBackgroundMonitoring(false)
            SenseBridgeForegroundService.stop(context)
            audioRecorderManager.stopListening()
        }
    }

    fun setSensitivity(isHighSensitivity: Boolean) {
        // High sensitivity for quiet room (55 dB); Normal for street (68 dB)
        energyGate.thresholdDb = if (isHighSensitivity) 55.0 else 68.0
    }

    override fun onCleared() {
        super.onCleared()
        // If background monitoring is disabled, stop listening; otherwise keep scanning!
        if (!audioRecorderManager.isBackgroundEnabled.value) {
            audioRecorderManager.stopListening()
        }
    }
}
