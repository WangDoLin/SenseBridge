package com.sensebridge.input.sound

import android.content.Context
import android.util.Log
import com.sensebridge.core.model.PriorityLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TensorFlow Lite audio classifier for recognizing environmental emergency sounds.
 */
@Singleton
class TFLiteAudioClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : SoundClassifier {

    companion object {
        private const val TAG = "TFLiteAudioClassifier"
        private const val MODEL_FILE = "yamnet.tflite"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.65f
    }

    private var classifier: AudioClassifier? = null
    override var isModelLoaded: Boolean = false
        private set

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            classifier = AudioClassifier.createFromFile(context, MODEL_FILE)
            isModelLoaded = true
            Log.i(TAG, "Successfully loaded $MODEL_FILE")
        } catch (e: IOException) {
            // Graceful fallback for development / test builds without downloaded weights
            Log.w(TAG, "Model file $MODEL_FILE not found in assets. Running in standby mode.", e)
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading audio model: ${e.message}", e)
            isModelLoaded = false
        }
    }

    override fun classify(audioSamples: FloatArray): AudioClassificationResult? {
        val activeClassifier = classifier
        if (activeClassifier != null && isModelLoaded) {
            return classifyWithTfLite(activeClassifier, audioSamples)
        }
        return null
    }

    private fun classifyWithTfLite(
        audioClassifier: AudioClassifier,
        samples: FloatArray
    ): AudioClassificationResult? {
        try {
            val audioRecord = audioClassifier.createAudioRecord()
            val tensorAudio = audioClassifier.createInputTensorAudio()
            tensorAudio.load(samples)

            val output = audioClassifier.classify(tensorAudio)
            val topCategory = output.firstOrNull()?.categories?.maxByOrNull { it.score }

            if (topCategory != null && topCategory.score >= MIN_CONFIDENCE_THRESHOLD) {
                return mapLabelToResult(topCategory.label, topCategory.score)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference execution failed: ${e.message}", e)
        }
        return null
    }

    /**
     * Maps raw AudioSet/YAMNet labels to Vietnamese titles, spoken text, and priorities.
     */
    fun mapLabelToResult(rawLabel: String, confidence: Float): AudioClassificationResult? {
        val lower = rawLabel.lowercase()

        return when {
            lower.contains("horn") || lower.contains("honk") -> AudioClassificationResult(
                label = "car_horn",
                vietnameseTitle = "Còi xe",
                spokenText = "Cảnh báo, có tiếng còi xe!",
                confidence = confidence,
                priority = PriorityLevel.CRITICAL_P0
            )

            lower.contains("siren") || lower.contains("emergency vehicle") -> AudioClassificationResult(
                label = "siren",
                vietnameseTitle = "Còi xe cấp cứu / Cảnh sát",
                spokenText = "Cảnh báo, có còi xe ưu tiên đến gần!",
                confidence = confidence,
                priority = PriorityLevel.CRITICAL_P0
            )

            lower.contains("fire alarm") || lower.contains("smoke detector") -> AudioClassificationResult(
                label = "fire_alarm",
                vietnameseTitle = "Báo cháy",
                spokenText = "Nguy hiểm, chuông báo cháy đang reo!",
                confidence = confidence,
                priority = PriorityLevel.CRITICAL_P0
            )

            lower.contains("doorbell") || lower.contains("ding-dong") || lower.contains("knock") -> AudioClassificationResult(
                label = "doorbell",
                vietnameseTitle = "Chuông cửa / Gõ cửa",
                spokenText = "Có tiếng chuông cửa hoặc gõ cửa.",
                confidence = confidence,
                priority = PriorityLevel.ATTENTION_P2
            )

            lower.contains("baby") || lower.contains("crying") || lower.contains("infant cry") -> AudioClassificationResult(
                label = "baby_cry",
                vietnameseTitle = "Tiếng em bé khóc",
                spokenText = "Có tiếng em bé đang khóc.",
                confidence = confidence,
                priority = PriorityLevel.WARNING_P1
            )

            lower.contains("bark") || lower.contains("dog") -> AudioClassificationResult(
                label = "dog_bark",
                vietnameseTitle = "Tiếng chó sủa",
                spokenText = "Có tiếng chó sủa gần đây.",
                confidence = confidence,
                priority = PriorityLevel.INFO_P3
            )

            else -> null // Ignore non-emergency environmental noise
        }
    }

    override fun release() {
        try {
            // Task Library AudioClassifier cleanup
            classifier = null
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio classifier resources", e)
        }
    }
}
