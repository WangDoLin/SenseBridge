package com.sensebridge.input.sound

import com.sensebridge.core.model.PriorityLevel

/**
 * Result of an audio classification inference.
 */
data class AudioClassificationResult(
    val label: String,
    val vietnameseTitle: String,
    val spokenText: String,
    val confidence: Float,
    val priority: PriorityLevel
)

/**
 * Interface defining contract for audio classification models (YAMNet, LiteRT, or heuristics).
 */
interface SoundClassifier {
    /**
     * Runs inference on the normalized float PCM audio buffer (-1.0 to +1.0).
     */
    fun classify(audioSamples: FloatArray): AudioClassificationResult?

    /**
     * Returns per-group scores for one window (multi-label). The default implementation wraps
     * [classify] so simpler classifiers keep working; YAMNet overrides it with all categories.
     */
    fun classifyGroups(audioSamples: FloatArray): Map<SoundGroup, Float> {
        val result = classify(audioSamples) ?: return emptyMap()
        val group = SoundGroup.forKey(result.label) ?: return emptyMap()
        return mapOf(group to result.confidence)
    }

    /**
     * Releases native inference sessions and buffers.
     */
    fun release()

    /**
     * Whether the model weights are loaded and ready.
     */
    val isModelLoaded: Boolean
}
