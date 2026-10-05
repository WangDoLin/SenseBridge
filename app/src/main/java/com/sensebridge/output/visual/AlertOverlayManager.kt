package com.sensebridge.output.visual

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class VisualAlertState(
    val activeEvent: SenseEvent? = null,
    val isFlashing: Boolean = false,
    val alertColorHex: Long = 0xFF000000
)

@Singleton
class AlertOverlayManager @Inject constructor() {

    private val _visualState = MutableStateFlow(VisualAlertState())
    val visualState: StateFlow<VisualAlertState> = _visualState.asStateFlow()

    fun displayAlert(event: SenseEvent) {
        val colorHex = when (event.priority) {
            PriorityLevel.CRITICAL_P0 -> 0xFFE53935
            PriorityLevel.WARNING_P1 -> 0xFFFDD835
            PriorityLevel.ATTENTION_P2 -> 0xFF1E88E5
            PriorityLevel.INFO_P3 -> 0xFF43A047
        }

        _visualState.value = VisualAlertState(
            activeEvent = event,
            isFlashing = event.priority.isUrgent && event.source == SensorySource.AUDIO_CLASSIFIER,
            alertColorHex = colorHex
        )
    }

    fun dismissAlert() {
        _visualState.value = VisualAlertState()
    }
}
