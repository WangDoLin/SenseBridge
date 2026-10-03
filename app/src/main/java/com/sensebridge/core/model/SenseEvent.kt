package com.sensebridge.core.model

import java.util.UUID

/**
 * Universal sensory event contract within SenseBridge.
 *
 * @param id Unique identifier for the event.
 * @param source The sensor or origin producing this event.
 * @param label The raw identifier (e.g., "car_horn", "person", "doorbell").
 * @param displayTitle Human-readable label for UI display.
 * @param spokenText Natural language sentence to be read aloud via TTS.
 * @param confidence Confidence score from the AI model (0.0 to 1.0).
 * @param priority Relative importance determining haptic and preemption logic.
 * @param spatialDirection Spatial positioning relative to user.
 * @param timestamp System time when event was generated.
 * @param trackingId Optional ML Kit stream tracking ID to avoid repetitive announcements.
 * @param metadata Additional sensor-specific attributes.
 */
data class SenseEvent(
    val id: String = UUID.randomUUID().toString(),
    val source: SensorySource,
    val label: String,
    val displayTitle: String,
    val spokenText: String,
    val confidence: Float,
    val priority: PriorityLevel,
    val spatialDirection: SpatialDirection = SpatialDirection.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val trackingId: Int? = null,
    val metadata: Map<String, Any> = emptyMap()
)
