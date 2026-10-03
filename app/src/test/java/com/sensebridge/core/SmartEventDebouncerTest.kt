package com.sensebridge.core

import com.sensebridge.core.engine.SmartEventDebouncer
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartEventDebouncerTest {

    private lateinit var debouncer: SmartEventDebouncer

    @Before
    fun setUp() {
        debouncer = SmartEventDebouncer(defaultWindowMs = 4000L, criticalP0WindowMs = 1000L)
    }

    @Test
    fun shouldProcess_firstEvent_returnsTrue() {
        val event = SenseEvent(
            source = SensorySource.AUDIO_CLASSIFIER,
            label = "doorbell",
            displayTitle = "Chuông cửa",
            spokenText = "Có tiếng chuông cửa",
            confidence = 0.9f,
            priority = PriorityLevel.ATTENTION_P2,
            spatialDirection = SpatialDirection.AHEAD,
            timestamp = 1000L
        )

        assertTrue(debouncer.shouldProcess(event))
    }

    @Test
    fun shouldProcess_identicalEventWithinWindow_returnsFalse() {
        val event1 = SenseEvent(
            source = SensorySource.AUDIO_CLASSIFIER,
            label = "doorbell",
            displayTitle = "Chuông cửa",
            spokenText = "Có tiếng chuông cửa",
            confidence = 0.9f,
            priority = PriorityLevel.ATTENTION_P2,
            spatialDirection = SpatialDirection.AHEAD,
            timestamp = 1000L
        )
        val event2 = event1.copy(timestamp = 2000L) // 1 second later

        assertTrue(debouncer.shouldProcess(event1))
        assertFalse(debouncer.shouldProcess(event2))
    }

    @Test
    fun shouldProcess_differentDirection_returnsTrue() {
        val event1 = SenseEvent(
            source = SensorySource.VISION_OBJECT_DETECTOR,
            label = "person",
            displayTitle = "Người",
            spokenText = "Có người bên trái",
            confidence = 0.85f,
            priority = PriorityLevel.ATTENTION_P2,
            spatialDirection = SpatialDirection.LEFT,
            timestamp = 1000L
        )
        val event2 = event1.copy(
            spatialDirection = SpatialDirection.RIGHT,
            spokenText = "Có người bên phải",
            timestamp = 1500L
        )

        assertTrue(debouncer.shouldProcess(event1))
        assertTrue(debouncer.shouldProcess(event2))
    }

    @Test
    fun shouldProcess_criticalP0PassesThroughAfterMicroCooldown() {
        val p0Event1 = SenseEvent(
            source = SensorySource.AUDIO_CLASSIFIER,
            label = "fire_alarm",
            displayTitle = "Báo cháy",
            spokenText = "Báo cháy khẩn cấp!",
            confidence = 0.99f,
            priority = PriorityLevel.CRITICAL_P0,
            timestamp = 1000L
        )
        val p0Event2 = p0Event1.copy(timestamp = 1200L) // only 200ms later -> debounce microburst
        val p0Event3 = p0Event1.copy(timestamp = 2100L) // 1100ms later -> pass through

        assertTrue(debouncer.shouldProcess(p0Event1))
        assertFalse(debouncer.shouldProcess(p0Event2))
        assertTrue(debouncer.shouldProcess(p0Event3))
    }

    @Test
    fun shouldProcess_trackedObjectAcrossFrames_suppressesRepeatedSpeech() {
        val frame1Object = SenseEvent(
            source = SensorySource.VISION_OBJECT_DETECTOR,
            label = "car",
            displayTitle = "Xe ô tô",
            spokenText = "Có xe phía trước",
            confidence = 0.92f,
            priority = PriorityLevel.WARNING_P1,
            trackingId = 42,
            timestamp = 1000L
        )
        val frame2Object = frame1Object.copy(timestamp = 1200L)

        assertTrue(debouncer.shouldProcess(frame1Object))
        assertFalse(debouncer.shouldProcess(frame2Object))
    }
}
