package com.sensebridge.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.UserProfile
import com.sensebridge.data.repository.UserPreferencesRepository
import com.sensebridge.output.audio.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPronoun = MutableStateFlow("bạn")
    val userPronoun: StateFlow<String> = _userPronoun.asStateFlow()

    private val _aiName = MutableStateFlow("SenseBridge")
    val aiName: StateFlow<String> = _aiName.asStateFlow()

    private val _aiPronoun = MutableStateFlow("tôi")
    val aiPronoun: StateFlow<String> = _aiPronoun.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.userProfileFlow.collect { profile ->
                if (_userName.value.isBlank() && profile.userName != "Bạn") {
                    _userName.value = profile.userName
                }
                _userPronoun.value = profile.userPronoun
                _aiName.value = profile.aiName
                _aiPronoun.value = profile.aiPronoun
            }
        }
    }

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setUserPronoun(pronoun: String) {
        _userPronoun.value = pronoun
    }

    fun setAiName(name: String) {
        _aiName.value = name
    }

    fun setAiPronoun(pronoun: String) {
        _aiPronoun.value = pronoun
    }

    fun getPreviewGreeting(): String {
        val profile = UserProfile(
            userName = _userName.value.ifBlank { "Bạn" },
            userPronoun = _userPronoun.value,
            aiName = _aiName.value.ifBlank { "SenseBridge" },
            aiPronoun = _aiPronoun.value
        )
        return profile.buildGreeting()
    }

    fun speakPreview() {
        textToSpeechManager.speak(getPreviewGreeting(), PriorityLevel.ATTENTION_P2)
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        val finalName = _userName.value.trim().ifBlank { "Bạn" }
        val finalPronoun = _userPronoun.value.trim().ifBlank { "bạn" }
        val finalAiName = _aiName.value.trim().ifBlank { "SenseBridge" }
        val finalAiPronoun = _aiPronoun.value.trim().ifBlank { "tôi" }

        viewModelScope.launch {
            preferencesRepository.updateUserProfile(
                userName = finalName,
                userPronoun = finalPronoun,
                aiName = finalAiName,
                aiPronoun = finalAiPronoun
            )
            preferencesRepository.setOnboardingCompleted(true)

            val greeting = UserProfile(
                userName = finalName,
                userPronoun = finalPronoun,
                aiName = finalAiName,
                aiPronoun = finalAiPronoun,
                isOnboardingCompleted = true
            ).buildGreeting()
            textToSpeechManager.speak(greeting, PriorityLevel.ATTENTION_P2)

            onCompleted()
        }
    }
}
