package com.sensebridge.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.core.dispatcher.OutputConfiguration
import com.sensebridge.core.dispatcher.SensoryDispatcher
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.UserProfile
import com.sensebridge.core.monitoring.MonitoringCoordinator
import com.sensebridge.data.repository.UserPreferencesRepository
import com.sensebridge.input.sound.DecibelEnergyGate
import com.sensebridge.output.audio.TextToSpeechManager
import com.sensebridge.output.audio.VoicePersona
import com.sensebridge.output.haptic.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dispatcher: SensoryDispatcher,
    private val energyGate: DecibelEnergyGate,
    private val eventEngine: EventEngine,
    private val monitoringCoordinator: MonitoringCoordinator,
    private val preferencesRepository: UserPreferencesRepository,
    private val hapticManager: HapticManager,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    val config: StateFlow<OutputConfiguration> = dispatcher.config
    val isBackgroundEnabled: StateFlow<Boolean> = monitoringCoordinator.isBackgroundEnabled

    private val _thresholdDb = MutableStateFlow(energyGate.thresholdDb)
    val thresholdDb: StateFlow<Double> = _thresholdDb.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.0f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeechRate = MutableStateFlow(1.0f)
    val voiceSpeechRate: StateFlow<Float> = _voiceSpeechRate.asStateFlow()

    private val _selectedPersona = MutableStateFlow(VoicePersona.DEFAULT)
    val selectedPersona: StateFlow<VoicePersona> = _selectedPersona.asStateFlow()

    val userProfile: StateFlow<UserProfile> = preferencesRepository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile.DEFAULT)

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _thresholdDb.value = prefs.thresholdDb
                _voicePitch.value = prefs.voicePitch
                _voiceSpeechRate.value = prefs.voiceSpeechRate
                val persona = VoicePersona.fromId(prefs.voicePersona)
                _selectedPersona.value = persona
                textToSpeechManager.setPitch(prefs.voicePitch)
                textToSpeechManager.setSpeechRate(prefs.voiceSpeechRate)
            }
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        dispatcher.updateConfiguration(config.value.copy(enableHaptic = enabled))
    }

    fun setVoiceEnabled(enabled: Boolean) {
        dispatcher.updateConfiguration(config.value.copy(enableVoice = enabled))
    }

    fun setVisualEnabled(enabled: Boolean) {
        dispatcher.updateConfiguration(config.value.copy(enableVisual = enabled))
    }

    fun toggleBackgroundMonitoring(context: Context? = null) {
        val willEnable = !isBackgroundEnabled.value
        monitoringCoordinator.setBackgroundMonitoring(willEnable)
    }

    fun updateThreshold(newThreshold: Double) {
        energyGate.thresholdDb = newThreshold
        _thresholdDb.value = newThreshold
        viewModelScope.launch {
            preferencesRepository.updateThresholdDb(newThreshold)
        }
    }

    fun testHapticVibration() {
        hapticManager.trigger(PriorityLevel.CRITICAL_P0)
    }

    fun setVoicePersona(persona: VoicePersona) {
        _selectedPersona.value = persona
        _voicePitch.value = persona.pitch
        _voiceSpeechRate.value = persona.speechRate
        textToSpeechManager.applyPersona(persona)
        viewModelScope.launch {
            preferencesRepository.updateVoicePersona(persona.id)
            preferencesRepository.updateVoicePitch(persona.pitch)
            preferencesRepository.updateVoiceSpeechRate(persona.speechRate)
        }
    }

    fun setVoicePitch(pitch: Float) {
        _voicePitch.value = pitch
        textToSpeechManager.setPitch(pitch)
        viewModelScope.launch {
            preferencesRepository.updateVoicePitch(pitch)
        }
    }

    fun setVoiceSpeechRate(rate: Float) {
        _voiceSpeechRate.value = rate
        textToSpeechManager.setSpeechRate(rate)
        viewModelScope.launch {
            preferencesRepository.updateVoiceSpeechRate(rate)
        }
    }

    fun testVoiceSpeech() {
        textToSpeechManager.speakSample()
    }

    fun updateUserProfile(userName: String, userPronoun: String, aiName: String, aiPronoun: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserProfile(userName, userPronoun, aiName, aiPronoun)
        }
    }

    fun clearAllLogs() {
        eventEngine.clearHistory()
    }
}
