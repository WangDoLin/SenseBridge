package com.sensebridge.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.model.SpatialDirection
import com.sensebridge.core.monitoring.MonitoringCoordinator
import com.sensebridge.output.audio.AudioRouteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val eventEngine: EventEngine,
    private val monitoringCoordinator: MonitoringCoordinator,
    private val audioRouteManager: AudioRouteManager
) : ViewModel() {

    val isPaused: StateFlow<Boolean> = eventEngine.isPaused
    val recentEvents: StateFlow<List<SenseEvent>> = eventEngine.recentEvents

    val isRecording: StateFlow<Boolean> = monitoringCoordinator.isRecording
    val currentDecibels: StateFlow<Double> = monitoringCoordinator.currentDecibels
    val isBackgroundEnabled: StateFlow<Boolean> = monitoringCoordinator.isBackgroundEnabled

    val isBluetoothConnected: StateFlow<Boolean> = audioRouteManager.isBluetoothConnected
    val connectedDeviceName: StateFlow<String?> = audioRouteManager.connectedDeviceName
    val isMutedForPrivacy: StateFlow<Boolean> = audioRouteManager.isMutedForPrivacy

    fun togglePause() {
        eventEngine.togglePause()
    }

    fun toggleBackgroundMonitoring(context: Context? = null) {
        monitoringCoordinator.toggleMonitoring()
    }

    fun unmutePrivacy() {
        audioRouteManager.uncheckPrivacyMute()
    }

    fun clearHistory() {
        eventEngine.clearHistory()
    }

    // --- Simulation triggers for testing Phase 1 Core Pipeline ---

    fun simulateLoudNoiseP0() {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AUDIO_CLASSIFIER,
                label = "loud_sound_p0",
                displayTitle = "TIẾNG ĐỘNG RẤT LỚN (88 dB)",
                spokenText = "Cảnh báo nguy hiểm, phát hiện tiếng động rất lớn xung quanh!",
                confidence = 0.98f,
                priority = PriorityLevel.CRITICAL_P0,
                spatialDirection = SpatialDirection.AHEAD
            )
        )
    }

    fun simulateHornP0() {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AUDIO_CLASSIFIER,
                label = "car_horn",
                displayTitle = "Còi xe khẩn cấp",
                spokenText = "Cảnh báo, có tiếng còi xe lớn!",
                confidence = 0.94f,
                priority = PriorityLevel.CRITICAL_P0,
                spatialDirection = SpatialDirection.AHEAD
            )
        )
    }

    fun simulateFireAlarmP0() {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AUDIO_CLASSIFIER,
                label = "fire_alarm",
                displayTitle = "Chuông báo cháy",
                spokenText = "Cảnh báo nguy hiểm, chuông báo cháy đang reo!",
                confidence = 0.98f,
                priority = PriorityLevel.CRITICAL_P0,
                spatialDirection = SpatialDirection.AHEAD
            )
        )
    }

    fun simulatePersonLeftP2() {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.VISION_OBJECT_DETECTOR,
                label = "person",
                displayTitle = "Người",
                spokenText = "Có người ở bên trái.",
                confidence = 0.88f,
                priority = PriorityLevel.ATTENTION_P2,
                spatialDirection = SpatialDirection.LEFT
            )
        )
    }

    fun simulateDoorbellP2() {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AUDIO_CLASSIFIER,
                label = "doorbell",
                displayTitle = "Chuông cửa",
                spokenText = "Có tiếng chuông cửa.",
                confidence = 0.91f,
                priority = PriorityLevel.ATTENTION_P2,
                spatialDirection = SpatialDirection.AHEAD
            )
        )
    }
}
