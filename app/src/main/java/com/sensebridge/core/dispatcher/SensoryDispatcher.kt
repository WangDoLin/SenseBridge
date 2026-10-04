package com.sensebridge.core.dispatcher

import com.sensebridge.core.model.SenseEvent
import com.sensebridge.output.audio.TextToSpeechManager
import com.sensebridge.output.haptic.HapticManager
import com.sensebridge.output.visual.AlertOverlayManager
import com.sensebridge.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches verified SenseEvents to the appropriate sensory channels (Haptic, Voice, Visual)
 * strictly following the Haptic-First timing invariant.
 */
@Singleton
class SensoryDispatcher @Inject constructor(
    private val hapticManager: HapticManager,
    private val ttsManager: TextToSpeechManager,
    private val visualManager: AlertOverlayManager,
    private val preferencesRepository: UserPreferencesRepository
) {
    companion object {
        /** Cross-modal fused horn events ("car_horn_fused") reuse the horn vibration rhythm. */
        private const val FUSED_SUFFIX = "_fused"
    }

    private val dispatcherScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _config = MutableStateFlow(OutputConfiguration())
    val config: StateFlow<OutputConfiguration> = _config.asStateFlow()

    init {
        dispatcherScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _config.value = _config.value.copy(
                    enableHaptic = prefs.enableHaptic,
                    enableVoice = prefs.enableVoice,
                    enableVisual = prefs.enableVisual
                )
            }
        }
    }

    fun updateConfiguration(newConfig: OutputConfiguration) {
        _config.value = newConfig
        dispatcherScope.launch {
            preferencesRepository.updateHapticEnabled(newConfig.enableHaptic)
            preferencesRepository.updateVoiceEnabled(newConfig.enableVoice)
            preferencesRepository.updateVisualEnabled(newConfig.enableVisual)
        }
    }

    /**
     * Dispatches an event through active output channels.
     * Enforces the invariant: Haptic triggers instantly before TTS speech synthesis completes.
     */
    fun dispatch(event: SenseEvent) {
        val currentConfig = _config.value

        // 1. Haptic Feedback (Immediate hardware impulse)
        dispatchHapticOnly(event)

        // 2. Auditory Voice Synthesis
        if (currentConfig.enableVoice && event.priority.weight <= currentConfig.minPriorityToSpeak.weight) {
            if (event.source == com.sensebridge.core.model.SensorySource.COMMUNICATION_INPUT) {
                ttsManager.speakAac(event.spokenText, event.priority)
            } else {
                ttsManager.speak(event.spokenText, event.priority)
            }
        }

        // 3. Visual Screen Cue
        if (currentConfig.enableVisual) {
            visualManager.displayAlert(event)
        }
    }

    /**
     * Vibrates for [event] without speaking or showing a new visual alert.
     * Used for continuations of an ongoing horn/alarm episode.
     */
    fun dispatchHapticOnly(event: SenseEvent) {
        val currentConfig = _config.value
        if (currentConfig.enableHaptic && event.priority.weight <= currentConfig.minPriorityToVibrate.weight) {
            hapticManager.triggerForLabel(event.label.removeSuffix(FUSED_SUFFIX), event.priority)
        }
    }
}
