package com.sensebridge.ui.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensebridge.data.local.entity.SavedPhraseEntity
import com.sensebridge.data.repository.PhraseRepository
import com.sensebridge.input.speech.SpeechToTextManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunicationViewModel @Inject constructor(
    private val phraseRepository: PhraseRepository,
    private val speechToTextManager: SpeechToTextManager
) : ViewModel() {

    val allPhrases: StateFlow<List<SavedPhraseEntity>> = phraseRepository.allPhrases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isListeningToOpponent: StateFlow<Boolean> = speechToTextManager.isListening
    val opponentSpokenText: StateFlow<String?> = speechToTextManager.recognizedText
    val sttError: StateFlow<String?> = speechToTextManager.errorMessage

    init {
        viewModelScope.launch {
            phraseRepository.initializeDefaultPhrasesIfNeeded()
        }
    }

    fun speakPhrase(phrase: SavedPhraseEntity) {
        viewModelScope.launch {
            phraseRepository.speakPhrase(phrase)
        }
    }

    fun startListeningToOpponent() {
        speechToTextManager.startListening()
    }

    fun stopListeningToOpponent() {
        speechToTextManager.stopListening()
    }

    fun addCustomPhrase(text: String, isEmergency: Boolean = false) {
        if (text.isBlank()) return
        viewModelScope.launch {
            phraseRepository.addCustomPhrase(text, isEmergency)
        }
    }

    fun speakCustomText(text: String, isEmergency: Boolean = false) {
        if (text.isBlank()) return
        val tempPhrase = SavedPhraseEntity(
            vietnameseText = text.trim(),
            category = if (isEmergency) "emergency" else "custom",
            priority = if (isEmergency) com.sensebridge.core.model.PriorityLevel.WARNING_P1 else com.sensebridge.core.model.PriorityLevel.ATTENTION_P2
        )
        viewModelScope.launch {
            phraseRepository.speakPhrase(tempPhrase)
        }
    }

    fun deletePhrase(phrase: SavedPhraseEntity) {
        viewModelScope.launch {
            phraseRepository.deletePhrase(phrase)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechToTextManager.stopListening()
    }
}
