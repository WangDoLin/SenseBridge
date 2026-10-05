package com.sensebridge.core.ai

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import java.util.concurrent.ConcurrentLinkedQueue

data class ObservedObject(
    val label: String,
    val vietnameseLabel: String,
    val direction: SpatialDirection,
    val isNear: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ObservedSound(
    val label: String,
    val vietnameseLabel: String,
    val decibels: Double,
    val priority: PriorityLevel,
    val timestamp: Long = System.currentTimeMillis()
)

data class ObservedText(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SceneSnapshot(
    val objects: List<ObservedObject>,
    val sounds: List<ObservedSound>,
    val latestText: String?,
    val ambientDecibels: Double,
    val isNoisy: Boolean
)

class MultimodalSceneMemory(
    private val retentionPeriodMs: Long = DEFAULT_RETENTION_MS
) {
    companion object {
        const val DEFAULT_RETENTION_MS = 6000L
        const val NOISY_DECIBEL_THRESHOLD = 70.0
    }

    private val objectQueue = ConcurrentLinkedQueue<ObservedObject>()
    private val soundQueue = ConcurrentLinkedQueue<ObservedSound>()
    private var lastObservedText: ObservedText? = null
    private var currentAmbientDecibels: Double = 0.0

    fun recordObject(objectInfo: ObservedObject) {
        pruneExpired()
        objectQueue.add(objectInfo)
    }

    fun recordSound(soundInfo: ObservedSound) {
        pruneExpired()
        soundQueue.add(soundInfo)
    }

    fun recordText(text: String) {
        if (text.isNotBlank()) {
            lastObservedText = ObservedText(text = text.trim())
        }
    }

    fun updateAmbientDecibels(decibels: Double) {
        currentAmbientDecibels = decibels
    }

    fun getSnapshot(now: Long = System.currentTimeMillis()): SceneSnapshot {
        pruneExpired(now)
        val validText = lastObservedText?.takeIf { now - it.timestamp <= retentionPeriodMs }?.text
        return SceneSnapshot(
            objects = objectQueue.toList(),
            sounds = soundQueue.toList(),
            latestText = validText,
            ambientDecibels = currentAmbientDecibels,
            isNoisy = currentAmbientDecibels >= NOISY_DECIBEL_THRESHOLD
        )
    }

    fun clear() {
        objectQueue.clear()
        soundQueue.clear()
        lastObservedText = null
        currentAmbientDecibels = 0.0
    }

    private fun pruneExpired(now: Long = System.currentTimeMillis()) {
        objectQueue.removeIf { (now - it.timestamp) > retentionPeriodMs }
        soundQueue.removeIf { (now - it.timestamp) > retentionPeriodMs }
        if (lastObservedText != null && (now - lastObservedText!!.timestamp) > retentionPeriodMs) {
            lastObservedText = null
        }
    }
}
