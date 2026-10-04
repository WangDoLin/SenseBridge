package com.sensebridge.output.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.sensebridge.core.model.PriorityLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Text-To-Speech engine with priority queuing and preemption capabilities.
 */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRouteManager: AudioRouteManager
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val DEFAULT_SPEECH_RATE = 1.0f
        private const val DEFAULT_PITCH = 1.0f
    }

    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val vietnameseLocale = Locale("vi", "VN")
            val result = tts?.setLanguage(vietnameseLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Vietnamese TTS not fully supported; falling back to default locale.")
                tts?.language = Locale.getDefault()
            }

            tts?.setSpeechRate(DEFAULT_SPEECH_RATE)
            tts?.setPitch(DEFAULT_PITCH)
            _isReady.value = true
            Log.i(TAG, "TextToSpeech initialized successfully.")
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech. Status code: $status")
            _isReady.value = false
        }
    }

    /**
     * Speaks the given text according to priority level.
     * High priority events (P0/P1) flush the queue to preempt ongoing speech.
     */
    fun speak(text: String, priority: PriorityLevel) {
        if (!_isReady.value || text.isBlank()) return

        // Privacy mute hides everyday speech, but a blind user whose headset just died must still
        // hear life-safety warnings through the loudspeaker (safety outweighs privacy).
        if (audioRouteManager.isMutedForPrivacy.value && priority != PriorityLevel.CRITICAL_P0) {
            Log.d(TAG, "Suppressed TTS speech due to privacy mute: '$text'")
            return
        }

        val queueMode = if (priority.isUrgent) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        val utteranceId = "utterance_${priority.name}_${UUID.randomUUID()}"
        tts?.speak(text, queueMode, null, utteranceId)
    }

    /**
     * Halts speech playback immediately.
     */
    fun stopImmediately() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS playback", e)
        }
    }

    /**
     * Shuts down TTS resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }
}
