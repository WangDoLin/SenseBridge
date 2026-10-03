package com.sensebridge.core.model

/**
 * Priority levels for incoming sensory events.
 * P0 (CRITICAL) has the highest priority and will preempt lower-priority events.
 */
enum class PriorityLevel(val weight: Int) {
    CRITICAL_P0(0),
    WARNING_P1(1),
    ATTENTION_P2(2),
    INFO_P3(3);

    val isUrgent: Boolean
        get() = this == CRITICAL_P0 || this == WARNING_P1
}
