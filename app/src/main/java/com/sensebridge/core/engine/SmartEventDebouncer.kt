package com.sensebridge.core.engine

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent event debouncer to prevent sensory overload and repetitive alerts.
 *
 * @param defaultWindowMs Standard time window to suppress identical events (default: 4000ms).
 * @param criticalP0WindowMs Strict minimal cooldown for emergency alerts (default: 1000ms).
 */
class SmartEventDebouncer(
    private val defaultWindowMs: Long = 4000L,
    private val criticalP0WindowMs: Long = 1000L
) {
    private val recentEventTimestamps = ConcurrentHashMap<String, Long>()
    private val trackedObjects = ConcurrentHashMap<Int, Long>()

    /**
     * Evaluates whether an event should be processed or suppressed.
     *
     * @param event The candidate sensory event.
     * @return True if the event is novel or outside cooldown; False if suppressed.
     */
    fun shouldProcess(event: SenseEvent): Boolean {
        val now = event.timestamp

        // Handle tracking ID for persistent objects across frames
        event.trackingId?.let { trackId ->
            val lastSeen = trackedObjects[trackId]
            if (lastSeen != null && (now - lastSeen) < defaultWindowMs) {
                trackedObjects[trackId] = now
                return false
            }
            trackedObjects[trackId] = now
        }

        // P0 (Critical emergency) has high pass-through with brief 1s cooldown
        if (event.priority == PriorityLevel.CRITICAL_P0) {
            val lastP0Time = recentEventTimestamps["P0_CRITICAL_KEY"] ?: 0L
            if (now - lastP0Time >= criticalP0WindowMs) {
                recentEventTimestamps["P0_CRITICAL_KEY"] = now
                return true
            }
            return false
        }

        // Standard debounce key composed of source, label, and direction
        val compositeKey = "${event.source.name}_${event.label}_${event.spatialDirection.name}"
        val lastTimestamp = recentEventTimestamps[compositeKey]

        if (lastTimestamp == null || (now - lastTimestamp) >= defaultWindowMs) {
            recentEventTimestamps[compositeKey] = now
            return true
        }

        return false
    }

    /**
     * Clears all cached event history.
     */
    fun reset() {
        recentEventTimestamps.clear()
        trackedObjects.clear()
    }
}
