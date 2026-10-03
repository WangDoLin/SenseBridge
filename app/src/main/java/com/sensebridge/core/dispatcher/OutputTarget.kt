package com.sensebridge.core.dispatcher

/**
 * Target outputs supported by SenseBridge.
 */
enum class OutputChannel {
    HAPTIC,
    VOICE,
    VISUAL_SCREEN
}

/**
 * Output configuration matrix for active sensory modes.
 */
data class OutputConfiguration(
    val enableHaptic: Boolean = true,
    val enableVoice: Boolean = true,
    val enableVisual: Boolean = true,
    val minPriorityToVibrate: com.sensebridge.core.model.PriorityLevel = com.sensebridge.core.model.PriorityLevel.ATTENTION_P2,
    val minPriorityToSpeak: com.sensebridge.core.model.PriorityLevel = com.sensebridge.core.model.PriorityLevel.INFO_P3
)
