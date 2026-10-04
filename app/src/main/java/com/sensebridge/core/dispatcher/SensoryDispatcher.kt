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
    companion object {
        /** Cross-modal fused horn events ("car_horn_fused") reuse the horn vibration rhythm. */
        private const val FUSED_SUFFIX = "_fused"
    }

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
        dispatchHapticOnly(event)

        // 2. Auditory Voice Synthesis
        if (currentConfig.enableVoice && event.priority.weight <= currentConfig.minPriorityToSpeak.weight) {
            ttsManager.speak(event.spokenText, event.priority)
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
