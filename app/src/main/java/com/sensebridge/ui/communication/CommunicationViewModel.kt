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
        viewModelScope.launch {
            phraseRepository.addCustomPhrase(text, isEmergency)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechToTextManager.release()
    }
}
