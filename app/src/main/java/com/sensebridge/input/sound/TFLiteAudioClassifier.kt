package com.sensebridge.input.sound

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YAMNet (521 AudioSet classes) classifier.
 *
 * Accuracy design (see accuracy_review.md):
 * - Uses ALL categories (YAMNet is multi-label). A horn often scores high while "Vehicle"
 *   is top-1, so top-1-only logic missed it.
 * - Maps labels by EXACT name through [SoundGroup] (whitelist). Fans, speech, music, engines…
 *   belong to no group and therefore can never raise an alarm.
 */
@Singleton
class TFLiteAudioClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : SoundClassifier {

    companion object {
        private const val TAG = "TFLiteAudioClassifier"
        private const val MODEL_FILE = "yamnet.tflite"
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
            Log.i(TAG, "Successfully loaded $MODEL_FILE into memory.")
        } catch (e: IOException) {
            Log.w(TAG, "Model file $MODEL_FILE not accessible in assets.", e)
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading audio model: ${e.message}", e)
            isModelLoaded = false
        }
    }

    /**
     * Runs YAMNet on one ~0.975 s window and returns per-group scores (only emergency groups).
     *
     * @param audioSamples normalized mono PCM at 16 kHz.
     * @return empty map when the model is unavailable or nothing relevant was heard.
     */
    override fun classifyGroups(audioSamples: FloatArray): Map<SoundGroup, Float> {
        val activeClassifier = classifier
        if (activeClassifier == null || !isModelLoaded) return emptyMap()
        val categories = runInference(activeClassifier, audioSamples)
        return AudioLabelTaxonomy.scoreGroups(categories.map { it.label to it.score })
    }

    /**
     * Single-window classification kept for the [SoundClassifier] contract: returns the most
     * urgent group passing its threshold, without temporal confirmation.
     */
    override fun classify(audioSamples: FloatArray): AudioClassificationResult? {
        val passing = AudioLabelTaxonomy.passingGroups(classifyGroups(audioSamples))
        val group = AudioLabelTaxonomy.mostUrgent(passing) ?: return null
        return AudioLabelTaxonomy.toResult(group, passing.getValue(group))
    }

    /**
     * Maps one raw YAMNet label to a result using exact matching.
     * "French horn" or "Engine knocking" return null instead of car horn / door knock.
     */
    fun mapLabelToResult(rawLabel: String, confidence: Float): AudioClassificationResult? =
        AudioLabelTaxonomy.mapLabel(rawLabel, confidence)

    private fun runInference(audioClassifier: AudioClassifier, samples: FloatArray): List<Category> {
        return try {
            val tensorAudio = audioClassifier.createInputTensorAudio()
            tensorAudio.load(samples)
            audioClassifier.classify(tensorAudio).firstOrNull()?.categories.orEmpty()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid audio buffer for inference: ${e.message}", e)
            emptyList()
        } catch (e: RuntimeException) {
            // Native TFLite failures must not kill the background recording coroutine
            Log.e(TAG, "Inference execution failed: ${e.message}", e)
            emptyList()
        }
    }

    override fun release() {
        try {
            classifier?.close()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error releasing audio classifier resources", e)
        } finally {
            classifier = null
            isModelLoaded = false
        }
    }
}
