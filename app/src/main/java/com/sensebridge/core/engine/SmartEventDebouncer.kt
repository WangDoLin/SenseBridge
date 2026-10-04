package com.sensebridge.core.engine

import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import java.util.concurrent.ConcurrentHashMap

/**
 * Outcome of debouncing a single event.
 */
enum class DebounceDecision {
    /** First detection of a sound/object episode: vibrate + speak + notify + record history. */
    NEW_EPISODE,

    /** The same urgent sound is still going on: vibrate again only (no TTS, no history spam). */
    CONTINUATION,

    /** Duplicate that should be dropped entirely. */
    SUPPRESS
}

/**
 * Episode-based event debouncer.
 *
 * Urgent audio events (P0/P1 such as car horns, sirens, fire alarms) are grouped into episodes:
 * - A detection after a silence gap longer than [episodeGapMs] starts a NEW episode.
 * - Detections inside an ongoing episode are CONTINUATIONs, so the phone keeps vibrating for
 *   every horn honk while TTS and notifications fire only once per episode.
 * - A very long episode (continuous alarm) is re-announced every [reannounceIntervalMs].
 *
 * All other events keep the classic per-key suppression window [defaultWindowMs].
 */
class SmartEventDebouncer(
    private val defaultWindowMs: Long = DEFAULT_WINDOW_MS,
    private val episodeGapMs: Long = EPISODE_GAP_MS,
    private val hapticRepeatIntervalMs: Long = HAPTIC_REPEAT_INTERVAL_MS,
    private val reannounceIntervalMs: Long = REANNOUNCE_INTERVAL_MS
) {
    companion object {
        const val DEFAULT_WINDOW_MS = 4000L
        const val EPISODE_GAP_MS = 2500L
        /** Slightly shorter than the ~1.3 s P0 waveform so vibration feels continuous. */
        const val HAPTIC_REPEAT_INTERVAL_MS = 1000L
        const val REANNOUNCE_INTERVAL_MS = 15000L
        private const val FUSED_SUFFIX = "_fused"
    }

    private data class Episode(val startedAt: Long, var lastSeenAt: Long, var lastHapticAt: Long)

    private val recentEventTimestamps = ConcurrentHashMap<String, Long>()
    private val trackedObjects = ConcurrentHashMap<Int, Long>()
    private val episodes = ConcurrentHashMap<String, Episode>()

    /**
     * Classifies [event] as a new episode, a continuation of an ongoing one, or a duplicate.
     */
    fun evaluate(event: SenseEvent): DebounceDecision {
        if (isUrgentAudio(event)) return evaluateEpisode(event)
        if (isRepeatedTrackedObject(event)) return DebounceDecision.SUPPRESS
        return evaluateWindow(event)
    }

    /**
     * Backwards-compatible boolean API: true only for events that deserve a full announcement.
     */
    fun shouldProcess(event: SenseEvent): Boolean = evaluate(event) == DebounceDecision.NEW_EPISODE

    private fun isUrgentAudio(event: SenseEvent): Boolean =
        event.source == SensorySource.AUDIO_CLASSIFIER && event.priority.isUrgent

    private fun evaluateEpisode(event: SenseEvent): DebounceDecision {
        val now = event.timestamp
        val key = event.label.removeSuffix(FUSED_SUFFIX)
        val episode = episodes[key]

        val isNewEpisode = episode == null ||
            now - episode.lastSeenAt > episodeGapMs ||
            now - episode.startedAt >= reannounceIntervalMs
        if (isNewEpisode) {
            episodes[key] = Episode(startedAt = now, lastSeenAt = now, lastHapticAt = now)
            return DebounceDecision.NEW_EPISODE
        }

        val active = episode ?: return DebounceDecision.SUPPRESS
        active.lastSeenAt = now
        if (now - active.lastHapticAt >= hapticRepeatIntervalMs) {
            active.lastHapticAt = now
            return DebounceDecision.CONTINUATION
        }
        return DebounceDecision.SUPPRESS
    }

    private fun isRepeatedTrackedObject(event: SenseEvent): Boolean {
        val trackId = event.trackingId ?: return false
        val now = event.timestamp
        val lastSeen = trackedObjects[trackId]
        trackedObjects[trackId] = now
        return lastSeen != null && (now - lastSeen) < defaultWindowMs
    }

    private fun evaluateWindow(event: SenseEvent): DebounceDecision {
        val now = event.timestamp
        val compositeKey = "${event.source.name}_${event.label}_${event.spatialDirection.name}"
        val lastTimestamp = recentEventTimestamps[compositeKey]
        if (lastTimestamp == null || (now - lastTimestamp) >= defaultWindowMs) {
            recentEventTimestamps[compositeKey] = now
            return DebounceDecision.NEW_EPISODE
        }
        return DebounceDecision.SUPPRESS
    }

    /**
     * Clears all cached event history and open episodes.
     */
    fun reset() {
        recentEventTimestamps.clear()
        trackedObjects.clear()
        episodes.clear()
    }
}
