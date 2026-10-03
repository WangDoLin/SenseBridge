package com.sensebridge.ui.blind

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.input.vision.CameraManager
import com.sensebridge.output.audio.AudioRouteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BlindAssistViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val eventEngine: EventEngine,
    private val audioRouteManager: AudioRouteManager
) : ViewModel() {

    private val _isCameraRunning = MutableStateFlow(false)
    val isCameraRunning: StateFlow<Boolean> = _isCameraRunning.asStateFlow()

    val isBluetoothConnected: StateFlow<Boolean> = audioRouteManager.isBluetoothConnected
    val connectedDeviceName: StateFlow<String?> = audioRouteManager.connectedDeviceName

    val recentVisionEvents: StateFlow<List<SenseEvent>> = eventEngine.recentEvents
        .map { list -> list.filter { it.source == SensorySource.VISION_OBJECT_DETECTOR } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastAnnouncedEvent: StateFlow<SenseEvent?> = recentVisionEvents
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startVision(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        cameraManager.startCamera(lifecycleOwner, previewView)
        _isCameraRunning.value = true
    }

    fun stopVision() {
        cameraManager.stopCamera()
        _isCameraRunning.value = false
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        if (_isCameraRunning.value) {
            stopVision()
        } else {
            startVision(lifecycleOwner, previewView)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.stopCamera()
    }
}
