package com.sensebridge.core.engine

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.model.SpatialDirection
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Fuses cross-modal inputs (e.g., Vision Car + Audio Horn) within a short correlation window.
 */
class MultimodalSynthesizer(
    private val fusionWindowMs: Long = 800L
) {
    private val recentEvents = ConcurrentLinkedQueue<SenseEvent>()

    /**
     * Checks if the incoming event can be fused with a recent cross-modal event.
     * Returns the fused SenseEvent if correlation is found, or the original event otherwise.
     */
    fun processOrFuse(incoming: SenseEvent): SenseEvent {
        cleanOldEvents(incoming.timestamp)

        // Scenario: Car detected visually + Horn detected acoustically
        if (incoming.source == SensorySource.AUDIO_CLASSIFIER && incoming.label.contains("horn")) {
            val matchingCar = recentEvents.firstOrNull {
                it.source == SensorySource.VISION_OBJECT_DETECTOR && it.label == "car"
            }
            if (matchingCar != null) {
                recentEvents.remove(matchingCar)
                return createFusedCarHornEvent(matchingCar.spatialDirection, incoming.timestamp)
            }
        } else if (incoming.source == SensorySource.VISION_OBJECT_DETECTOR && incoming.label == "car") {
            val matchingHorn = recentEvents.firstOrNull {
                it.source == SensorySource.AUDIO_CLASSIFIER && it.label.contains("horn")
            }
            if (matchingHorn != null) {
                recentEvents.remove(matchingHorn)
                return createFusedCarHornEvent(incoming.spatialDirection, incoming.timestamp)
            }
        }

        recentEvents.add(incoming)
        return incoming
    }

    private fun createFusedCarHornEvent(direction: SpatialDirection, timestamp: Long): SenseEvent {
        val directionText = if (direction != SpatialDirection.UNKNOWN) direction.vietnameseLabel else "phía trước"
        return SenseEvent(
            source = SensorySource.AUDIO_CLASSIFIER,
            label = "car_horn_fused",
            displayTitle = "Xe đang bấm còi ($directionText)",
            spokenText = "Cảnh báo, xe $directionText đang bấm còi!",
            confidence = 0.95f,
            priority = PriorityLevel.CRITICAL_P0,
            spatialDirection = direction,
            timestamp = timestamp
        )
    }

    private fun cleanOldEvents(now: Long) {
        recentEvents.removeIf { (now - it.timestamp) > fusionWindowMs }
    }
}
