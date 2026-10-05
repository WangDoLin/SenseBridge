package com.sensebridge.core.ai

import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.output.audio.TextToSpeechManager
import com.sensebridge.output.haptic.HapticManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultimodalAiAssistantEngine @Inject constructor(
    private val eventEngine: EventEngine,
    private val textToSpeechManager: TextToSpeechManager,
    private val hapticManager: HapticManager,
    private val neuralClassifier: SenseAiNeuralClassifier? = null,
    private val slmEngine: OnDeviceSlmInferenceEngine? = null,
    private val continualLearningEngine: ContinualLearningEngine? = null
) {
    companion object {
        private const val DEFAULT_COOLDOWN_MS = 7000L
        private const val CRITICAL_COOLDOWN_MS = 2500L
    }

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sceneMemory = MultimodalSceneMemory()
    private val reasoner = AiMultimodalReasoner()
    private val dialogueModel = SenseAiDialogueModel(
        neuralClassifier = neuralClassifier,
        slmEngine = slmEngine,
        continualLearningEngine = continualLearningEngine
    )
    private val lastInsightTimestamps = ConcurrentHashMap<String, Long>()

    private val _latestInsight = MutableStateFlow<AiProactiveInsight?>(null)
    val latestInsight: StateFlow<AiProactiveInsight?> = _latestInsight.asStateFlow()

    private val _isObserving = MutableStateFlow(false)
    val isObserving: StateFlow<Boolean> = _isObserving.asStateFlow()

    private var observationJob: Job? = null

    init {
        startObservation()
    }

    fun startObservation() {
        if (_isObserving.value) return
        _isObserving.value = true

        observationJob?.cancel()
        observationJob = engineScope.launch {
            eventEngine.recentEvents.collect { events ->
                val latest = events.firstOrNull() ?: return@collect
                processIncomingEvent(latest)
            }
        }
    }

    fun stopObservation() {
        _isObserving.value = false
        observationJob?.cancel()
        observationJob = null
    }

    fun updateAmbientDecibels(decibels: Double) {
        sceneMemory.updateAmbientDecibels(decibels)
    }

    fun askAssistant(userQuery: String): AiQueryAnswer {
        val response = converse(userQuery)
        return AiQueryAnswer(
            answerText = response.replyText,
            priority = response.priority
        )
    }

    suspend fun askAssistantAsync(userQuery: String): AiQueryAnswer {
        val response = converseAsync(userQuery)
        return AiQueryAnswer(
            answerText = response.replyText,
            priority = response.priority
        )
    }

    fun converse(userQuery: String): DialogueResponse {
        val snapshot = sceneMemory.getSnapshot()
        val response = dialogueModel.converse(userQuery, snapshot)
        textToSpeechManager.speak(response.replyText, response.priority)
        return response
    }

    suspend fun converseAsync(userQuery: String): DialogueResponse {
        val snapshot = sceneMemory.getSnapshot()
        val response = dialogueModel.converseAsync(userQuery, snapshot)
        textToSpeechManager.speak(response.replyText, response.priority)
        return response
    }

    fun getCurrentSituation(): SituationClassificationResult =
        dialogueModel.classifySituation(sceneMemory.getSnapshot())

    fun getSceneSnapshot(): SceneSnapshot = sceneMemory.getSnapshot()

    fun resetMemory() {
        sceneMemory.clear()
        dialogueModel.resetDialogue()
        lastInsightTimestamps.clear()
        _latestInsight.value = null
    }

    private fun processIncomingEvent(event: SenseEvent) {
        when (event.source) {
            SensorySource.VISION_OBJECT_DETECTOR -> {
                val isNear = event.spokenText.contains("ở gần", ignoreCase = true)
                sceneMemory.recordObject(
                    ObservedObject(
                        label = event.label,
                        vietnameseLabel = event.displayTitle,
                        direction = event.spatialDirection,
                        isNear = isNear,
                        timestamp = event.timestamp
                    )
                )
                evaluateSceneAndReact()
            }
            SensorySource.VISION_OCR -> {
                sceneMemory.recordText(event.spokenText)
                evaluateSceneAndReact()
            }
            SensorySource.AUDIO_CLASSIFIER -> {
                val db = (event.metadata["decibels"] as? Number)?.toDouble() ?: 60.0
                sceneMemory.recordSound(
                    ObservedSound(
                        label = event.label,
                        vietnameseLabel = event.displayTitle,
                        decibels = db,
                        priority = event.priority,
                        timestamp = event.timestamp
                    )
                )
                evaluateSceneAndReact()
            }
            SensorySource.COMMUNICATION_INPUT,
            SensorySource.AI_ASSISTANT -> {}
        }
    }

    private fun evaluateSceneAndReact() {
        val snapshot = sceneMemory.getSnapshot()
        val insight = reasoner.evaluateSceneThreat(snapshot) ?: return

        val now = System.currentTimeMillis()
        val lastTime = lastInsightTimestamps[insight.insightType] ?: 0L
        val cooldown = if (insight.priority == PriorityLevel.CRITICAL_P0) CRITICAL_COOLDOWN_MS else DEFAULT_COOLDOWN_MS

        if (now - lastTime < cooldown) return

        lastInsightTimestamps[insight.insightType] = now
        _latestInsight.value = insight

        hapticManager.trigger(insight.priority)
        textToSpeechManager.speak(insight.spokenText, insight.priority)

        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AI_ASSISTANT,
                label = "ai_insight_${insight.insightType.lowercase()}",
                displayTitle = insight.title,
                spokenText = insight.spokenText,
                confidence = 0.98f,
                priority = insight.priority
            )
        )
    }
}
