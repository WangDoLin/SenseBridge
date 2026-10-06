package com.sensebridge.ui.home

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.core.ai.MultimodalAiAssistantEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.UserProfile
import com.sensebridge.data.repository.UserPreferencesRepository
import com.sensebridge.input.speech.SpeechToTextManager
import com.sensebridge.input.vision.CameraManager
import com.sensebridge.output.haptic.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class FloatingAssistantViewModel @Inject constructor(
    private val speechToTextManager: SpeechToTextManager,
    private val aiAssistantEngine: MultimodalAiAssistantEngine,
    private val cameraManager: CameraManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val hapticManager: HapticManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferencesRepository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile.DEFAULT)

    private val _isAssistantOpen = MutableStateFlow(false)
    val isAssistantOpen: StateFlow<Boolean> = _isAssistantOpen.asStateFlow()

    val isListening: StateFlow<Boolean> = speechToTextManager.isListening
    val recognizedSpeechText: StateFlow<String?> = speechToTextManager.recognizedText

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AssistantChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AssistantChatMessage>> = _chatMessages.asStateFlow()

    private var sttCollectionJob: Job? = null

    init {
        observeSpeechRecognition()
    }

    private fun observeSpeechRecognition() {
        sttCollectionJob?.cancel()
        sttCollectionJob = viewModelScope.launch {
            recognizedSpeechText.collect { text ->
                if (!text.isNullOrBlank() && !isListening.value) {
                    // Final speech text received
                    processUserQuery(text)
                }
            }
        }
    }

    fun openAssistant() {
        _isAssistantOpen.value = true
        hapticManager.trigger(PriorityLevel.INFO_P3)
    }

    fun closeAssistant() {
        stopListening()
        stopCamera()
        _isAssistantOpen.value = false
    }

    fun startListening() {
        hapticManager.trigger(PriorityLevel.ATTENTION_P2)
        speechToTextManager.startListening()
    }

    fun stopListening() {
        speechToTextManager.stopListening()
    }

    fun toggleListening() {
        if (isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        cameraManager.startCamera(lifecycleOwner, previewView)
        _isCameraActive.value = true
    }

    fun stopCamera() {
        cameraManager.stopCamera()
        _isCameraActive.value = false
    }

    fun processUserQuery(
        query: String,
        lifecycleOwner: LifecycleOwner? = null,
        previewView: PreviewView? = null
    ) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        // 1. Add user message
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(AssistantChatMessage(cleanQuery, isUser = true))
        _chatMessages.value = currentList

        val isVisionQuery = isObjectOrVisionQuery(cleanQuery)

        viewModelScope.launch {
            _isAiThinking.value = true

            // If query is about "Đây là cái gì", auto-activate camera if not running
            if (isVisionQuery && !_isCameraActive.value && lifecycleOwner != null) {
                startCamera(lifecycleOwner, previewView)
                // Small buffer for camera frames to propagate to object detector
                delay(600)
            }

            // 2. Query AI dialogue assistant
            val response = aiAssistantEngine.converseAsync(cleanQuery)
            _isAiThinking.value = false

            // 3. Add AI message
            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(AssistantChatMessage(response.replyText, isUser = false))
            _chatMessages.value = updatedList

            hapticManager.trigger(response.priority)
        }
    }

    private fun isObjectOrVisionQuery(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("đây là") ||
                lower.contains("cái gì") ||
                lower.contains("vật này") ||
                lower.contains("trước mặt") ||
                lower.contains("nhìn") ||
                lower.contains("xem") ||
                lower.contains("chữ gì") ||
                lower.contains("đồ gì")
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.stopCamera()
        speechToTextManager.stopListening()
    }
}
