package com.sensebridge.core.dispatcher

import com.sensebridge.core.model.SenseEvent
import com.sensebridge.output.audio.TextToSpeechManager
import com.sensebridge.output.haptic.HapticManager
import com.sensebridge.output.visual.AlertOverlayManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val visualManager: AlertOverlayManager
) {
    private val _config = MutableStateFlow(OutputConfiguration())
    val config: StateFlow<OutputConfiguration> = _config.asStateFlow()

    fun updateConfiguration(newConfig: OutputConfiguration) {
        _config.value = newConfig
    }

    /**
     * Dispatches an event through active output channels.
     * Enforces the invariant: Haptic triggers instantly before TTS speech synthesis completes.
     */
    fun dispatch(event: SenseEvent) {
        val currentConfig = _config.value

        // 1. Haptic Feedback (Immediate hardware impulse)
        if (currentConfig.enableHaptic && event.priority.weight <= currentConfig.minPriorityToVibrate.weight) {
            hapticManager.trigger(event.priority)
        }

        // 2. Auditory Voice Synthesis
        if (currentConfig.enableVoice && event.priority.weight <= currentConfig.minPriorityToSpeak.weight) {
            ttsManager.speak(event.spokenText, event.priority)
        }

        // 3. Visual Screen Cue
        if (currentConfig.enableVisual) {
            visualManager.displayAlert(event)
        }
    }
}
