package com.sensebridge.output.haptic

import com.sensebridge.core.model.PriorityLevel

/**
 * Interface defining haptic feedback capabilities for SenseBridge.
 */
interface HapticManager {
    /**
     * Triggers appropriate haptic feedback based on the priority level.
     */
    fun trigger(priority: PriorityLevel)

    /**
     * Immediately stops any ongoing vibration.
     */
    fun cancel()

    /**
     * Indicates whether the device hardware supports fine-grained amplitude modulation.
     */
    val hasAmplitudeControl: Boolean
}
