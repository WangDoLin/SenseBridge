package com.sensebridge.core.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnDeviceSlmInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: SlmModelManager
) {
    companion object {
        private const val TAG = "OnDeviceSlmEngine"
        private const val MAX_GENERATION_TOKENS = 256
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_TOP_K = 40
    }

    private val inferenceMutex = Mutex()
    private var llmInference: LlmInference? = null
    private var isInitialized = false

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            if (isInitialized && llmInference != null) return@withLock true

            val modelFile = modelManager.getModelFile() ?: run {
                Log.d(TAG, "SLM model file not found in device storage.")
                return@withLock false
            }

            if (!modelManager.isDeviceMemorySufficient()) {
                Log.w(TAG, "Device memory insufficient for SLM inference: ${modelManager.getAvailableMemoryMb()}MB available.")
                return@withLock false
            }

            return@withLock try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(MAX_GENERATION_TOKENS)
                    .setTemperature(DEFAULT_TEMPERATURE)
                    .setTopK(DEFAULT_TOP_K)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                isInitialized = true
                Log.i(TAG, "On-device SLM initialized successfully from ${modelFile.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LlmInference engine", e)
                llmInference = null
                isInitialized = false
                false
            }
        }
    }

    suspend fun generateResponse(
        userInput: String,
        snapshot: SceneSnapshot
    ): String? = withContext(Dispatchers.Default) {
        if (!isInitialized || llmInference == null) {
            val initialized = initialize()
            if (!initialized) return@withContext null
        }

        val prompt = formatMultimodalPrompt(userInput, snapshot)
        inferenceMutex.withLock {
            try {
                val response = llmInference?.generateResponse(prompt)
                response?.trim()
            } catch (e: Exception) {
                Log.e(TAG, "Inference error during text generation", e)
                null
            }
        }
    }

    fun isReady(): Boolean = isInitialized && llmInference != null

    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }

    fun formatMultimodalPrompt(userInput: String, snapshot: SceneSnapshot): String {
        val objectList = if (snapshot.objects.isNotEmpty()) {
            snapshot.objects.take(4).joinToString(", ") {
                "${it.vietnameseLabel} (${it.direction.vietnameseLabel})"
            }
        } else {
            "Không có vật cản lớn"
        }

        val ocrText = if (!snapshot.latestText.isNullOrBlank()) {
            snapshot.latestText
        } else {
            "Không có văn bản"
        }

        val soundInfo = if (snapshot.sounds.isNotEmpty()) {
            snapshot.sounds.take(2).joinToString(", ") { it.vietnameseLabel }
        } else {
            "Môi trường bình thường"
        }

        return """
            <start_of_turn>user
            Bạn là SenseAI, trợ lý giác quan thông minh cho người khiếm thị/khiếm thính.
            Ngữ cảnh môi trường hiện tại:
            - Vật thể camera nhìn thấy: $objectList
            - Chữ camera đọc được: $ocrText
            - Âm thanh ghi nhận: $soundInfo
            - Mức ồn: ${snapshot.ambientDecibels.toInt()} dB

            Người dùng nói: $userInput
            Hãy trả lời bằng tiếng Việt tự nhiên, thân thiện và ngắn gọn trong 1 đến 2 câu.<end_of_turn>
            <start_of_turn>model
        """.trimIndent()
    }
}
