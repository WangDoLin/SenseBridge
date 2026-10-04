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
     * Vibrates with the rhythm assigned to [label] (see [HapticSignatures]), falling back to the
     * generic [priority] pattern when the label has no dedicated rhythm.
     */
    fun triggerForLabel(label: String, priority: PriorityLevel) = trigger(priority)

    /**
     * Immediately stops any ongoing vibration.
     */
    fun cancel()

    /**
     * Indicates whether the device hardware supports fine-grained amplitude modulation.
     */
    val hasAmplitudeControl: Boolean
}
