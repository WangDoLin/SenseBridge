package com.sensebridge.core.engine

import android.util.Log
import com.sensebridge.core.dispatcher.SensoryDispatcher
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.output.audio.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central nervous system of SenseBridge.
 * Ingests, debounces, synthesizes, and dispatches sensory events across modalities.
 */
@Singleton
class EventEngine @Inject constructor(
    private val debouncer: SmartEventDebouncer,
    private val synthesizer: MultimodalSynthesizer,
    private val dispatcher: SensoryDispatcher,
    private val ttsManager: TextToSpeechManager
) {
    companion object {
        private const val TAG = "EventEngine"
        private const val MAX_HISTORY_SIZE = 30
    }

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventChannel = Channel<SenseEvent>(capacity = Channel.UNLIMITED)

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<SenseEvent>>(emptyList())
    val recentEvents: StateFlow<List<SenseEvent>> = _recentEvents.asStateFlow()

    private val eventHistory = CopyOnWriteArrayList<SenseEvent>()

    init {
        // Single serialized pipeline ensures strict chronological order and eliminates race conditions
        engineScope.launch {
            for (event in eventChannel) {
                processIncomingEvent(event)
            }
        }
    }

    /**
     * Ingests a new candidate sensory event from any source.
     */
    fun submitEvent(rawEvent: SenseEvent) {
        val isUserAction = rawEvent.source == SensorySource.COMMUNICATION_INPUT ||
            rawEvent.source == SensorySource.VISION_OCR

        // User-initiated actions (AAC speech, OCR reading) must never be dropped even if sensing is paused
        if (_isPaused.value && !isUserAction) {
            Log.d(TAG, "Event dropped: EventEngine is currently paused.")
            return
        }

        eventChannel.trySend(rawEvent)
    }

    private fun processIncomingEvent(rawEvent: SenseEvent) {
        val fusedEvent = synthesizer.processOrFuse(rawEvent)

        when (debouncer.evaluate(fusedEvent)) {
            DebounceDecision.NEW_EPISODE -> announceNewEpisode(fusedEvent)
            // Ongoing horn/alarm: keep vibrating without restarting speech or spamming history
            DebounceDecision.CONTINUATION -> dispatcher.dispatchHapticOnly(fusedEvent)
            DebounceDecision.SUPPRESS ->
                Log.d(TAG, "Debounced event: ${fusedEvent.label} (${fusedEvent.source})")
        }
    }

    private fun announceNewEpisode(event: SenseEvent) {
        if (event.priority == PriorityLevel.CRITICAL_P0) {
            ttsManager.stopImmediately()
        }
        recordEvent(event)
        dispatcher.dispatch(event)
    }

    private fun recordEvent(event: SenseEvent) {
        eventHistory.add(0, event)
        if (eventHistory.size > MAX_HISTORY_SIZE) {
            eventHistory.removeAt(eventHistory.size - 1)
        }
        _recentEvents.value = eventHistory.toList()
    }

    /**
     * Toggles pause/active state for all sensing.
     */
    fun togglePause() {
        _isPaused.value = !_isPaused.value
        if (_isPaused.value) {
            ttsManager.stopImmediately()
        }
    }

    /**
     * Resets event history and debouncers.
     */
    fun clearHistory() {
        eventHistory.clear()
        _recentEvents.value = emptyList()
        debouncer.reset()
    }
}
