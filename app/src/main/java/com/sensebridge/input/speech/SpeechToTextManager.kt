package com.sensebridge.input.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages two-way Speech-to-Text conversion for communication assist.
 */
@Singleton
class SpeechToTextManager @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {

    companion object {
        private const val TAG = "SpeechToTextManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow<String?>(null)
    val recognizedText: StateFlow<String?> = _recognizedText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechToTextManager)
            }
        } else {
            Log.w(TAG, "SpeechRecognizer is not available on this device.")
        }
    }

    fun startListening() {
        val recognizer = speechRecognizer ?: run {
            _errorMessage.value = "Thiết bị không hỗ trợ nhận diện giọng nói."
            return
        }

        _errorMessage.value = null
        _recognizedText.value = null

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            recognizer.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SpeechRecognizer: ${e.message}", e)
            _isListening.value = false
            _errorMessage.value = "Lỗi khởi động mic: ${e.message}"
        }
    }

    fun stopListening() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        }
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "Không nghe rõ câu nói. Vui lòng nói lại gần hơn."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi kết nối mạng khi nhận dạng giọng nói."
            SpeechRecognizer.ERROR_AUDIO -> "Lỗi thu âm từ mic."
            SpeechRecognizer.ERROR_CLIENT -> "Đã hủy phiên nghe."
            else -> "Chưa nhận diện được âm thanh (Mã: $error)."
        }
        _errorMessage.value = message
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestText = matches?.firstOrNull()

        if (!bestText.isNullOrBlank()) {
            _recognizedText.value = bestText
            _errorMessage.value = null
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val currentText = partials?.firstOrNull()
        if (!currentText.isNullOrBlank()) {
            _recognizedText.value = currentText
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isListening.value = false
    }
}
