package com.sensebridge.output.visual

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State representing visual screen alert overlays.
 */
data class VisualAlertState(
    val activeEvent: SenseEvent? = null,
    val isFlashing: Boolean = false,
    val alertColorHex: Long = 0xFF000000
)

/**
 * Manages high-contrast visual radar and flash cues for Deaf Assist and general display.
 */
@Singleton
class AlertOverlayManager @Inject constructor() {

    private val _visualState = MutableStateFlow(VisualAlertState())
    val visualState: StateFlow<VisualAlertState> = _visualState.asStateFlow()

    fun displayAlert(event: SenseEvent) {
        val colorHex = when (event.priority) {
            PriorityLevel.CRITICAL_P0 -> 0xFFE53935 // Bright Red
            PriorityLevel.WARNING_P1 -> 0xFFFDD835  // Warning Yellow
            PriorityLevel.ATTENTION_P2 -> 0xFF1E88E5 // Brand Blue
            PriorityLevel.INFO_P3 -> 0xFF43A047     // Green/Neutral
        }

        _visualState.value = VisualAlertState(
            activeEvent = event,
            isFlashing = event.priority.isUrgent,
            alertColorHex = colorHex
        )
    }

    fun dismissAlert() {
        _visualState.value = VisualAlertState()
    }
}
