package com.sensebridge.core

import com.sensebridge.core.engine.DebounceDecision
import com.sensebridge.core.engine.SmartEventDebouncer
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartEventDebouncerTest {

    private lateinit var debouncer: SmartEventDebouncer

    private val doorbell = SenseEvent(
        source = SensorySource.AUDIO_CLASSIFIER,
        label = "doorbell",
        displayTitle = "Chuông cửa",
        spokenText = "Có tiếng chuông cửa",
        confidence = 0.9f,
        priority = PriorityLevel.ATTENTION_P2,
        spatialDirection = SpatialDirection.AHEAD,
        timestamp = 1000L
    )

    private val horn = SenseEvent(
        source = SensorySource.AUDIO_CLASSIFIER,
        label = "car_horn",
        displayTitle = "Còi xe",
        spokenText = "Cảnh báo, có tiếng còi xe!",
        confidence = 0.8f,
        priority = PriorityLevel.CRITICAL_P0,
        timestamp = 1000L
    )

    @Before
    fun setUp() {
        debouncer = SmartEventDebouncer(
            defaultWindowMs = 4000L,
            episodeGapMs = 2500L,
            hapticRepeatIntervalMs = 1000L,
            reannounceIntervalMs = 15000L
        )
    }

    @Test
    fun shouldProcess_firstEvent_returnsTrue() {
        assertTrue(debouncer.shouldProcess(doorbell))
    }

    @Test
    fun shouldProcess_identicalEventWithinWindow_returnsFalse() {
        assertTrue(debouncer.shouldProcess(doorbell))
        assertFalse(debouncer.shouldProcess(doorbell.copy(timestamp = 2000L)))
    }

    @Test
    fun shouldProcess_differentDirection_returnsTrue() {
        val left = doorbell.copy(source = SensorySource.VISION_OBJECT_DETECTOR, label = "person", spatialDirection = SpatialDirection.LEFT)
        val right = left.copy(spatialDirection = SpatialDirection.RIGHT, timestamp = 1500L)
        assertTrue(debouncer.shouldProcess(left))
        assertTrue(debouncer.shouldProcess(right))
    }

    @Test
    fun shouldProcess_trackedObjectAcrossFrames_suppressesRepeatedSpeech() {
        val car = doorbell.copy(source = SensorySource.VISION_OBJECT_DETECTOR, label = "car", trackingId = 42)
        assertTrue(debouncer.shouldProcess(car))
        assertFalse(debouncer.shouldProcess(car.copy(timestamp = 1200L)))
    }

    @Test
    fun hornEpisode_continuousHorn_keepsVibratingWithoutReannouncing() {
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(horn))
        assertEquals(DebounceDecision.SUPPRESS, debouncer.evaluate(horn.copy(timestamp = 1500L)))
        assertEquals(DebounceDecision.CONTINUATION, debouncer.evaluate(horn.copy(timestamp = 2000L)))
        assertEquals(DebounceDecision.CONTINUATION, debouncer.evaluate(horn.copy(timestamp = 3000L)))
    }

    @Test
    fun hornEpisode_everySeparateHonk_isAnnouncedAgain() {
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(horn))
        // Silence longer than the episode gap → a new honk is a brand new alert
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(horn.copy(timestamp = 4000L)))
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(horn.copy(timestamp = 7000L)))
    }

    @Test
    fun hornEpisode_neverSuppressedForever() {
        debouncer.evaluate(horn)
        var hapticCount = 0
        for (t in 1500L..10000L step 500L) {
            if (debouncer.evaluate(horn.copy(timestamp = t)) != DebounceDecision.SUPPRESS) hapticCount++
        }
        // 8.5 s of continuous horn must vibrate roughly every second
        assertTrue("Expected >= 8 vibrations, got $hapticCount", hapticCount >= 8)
    }

    @Test
    fun longAlarm_isReannouncedPeriodically() {
        val alarm = horn.copy(label = "fire_alarm")
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(alarm))
        var reannounced = false
        for (t in 2000L..16000L step 1000L) {
            if (debouncer.evaluate(alarm.copy(timestamp = t)) == DebounceDecision.NEW_EPISODE) reannounced = true
        }
        assertTrue(reannounced)
    }

    @Test
    fun fusedHorn_continuesSameEpisode() {
        debouncer.evaluate(horn)
        val fused = horn.copy(label = "car_horn_fused", timestamp = 2100L)
        assertEquals(DebounceDecision.CONTINUATION, debouncer.evaluate(fused))
    }

    @Test
    fun differentUrgentSounds_haveIndependentEpisodes() {
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(horn))
        assertEquals(DebounceDecision.NEW_EPISODE, debouncer.evaluate(horn.copy(label = "siren", timestamp = 1200L)))
    }
}
