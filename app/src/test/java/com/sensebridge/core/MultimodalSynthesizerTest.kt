package com.sensebridge.core

import com.sensebridge.core.engine.MultimodalSynthesizer
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultimodalSynthesizerTest {

    private lateinit var synthesizer: MultimodalSynthesizer

    @Before
    fun setUp() {
        synthesizer = MultimodalSynthesizer(fusionWindowMs = 800L)
    }

    @Test
    fun processOrFuse_carAndHornWithinWindow_fusesIntoEmergencyAlert() {
        val carEvent = SenseEvent(
            source = SensorySource.VISION_OBJECT_DETECTOR,
            label = "car",
            displayTitle = "Xe ô tô",
            spokenText = "Có xe phía trước",
            confidence = 0.9f,
            priority = PriorityLevel.WARNING_P1,
            spatialDirection = SpatialDirection.CENTER,
            timestamp = 1000L
        )

        val hornEvent = SenseEvent(
            source = SensorySource.AUDIO_CLASSIFIER,
            label = "car_horn",
            displayTitle = "Còi xe",
            spokenText = "Có tiếng còi xe",
            confidence = 0.95f,
            priority = PriorityLevel.CRITICAL_P0,
            timestamp = 1300L // 300ms after car was seen
        )

        synthesizer.processOrFuse(carEvent)
        val result = synthesizer.processOrFuse(hornEvent)

        assertEquals("car_horn_fused", result.label)
        assertEquals(PriorityLevel.CRITICAL_P0, result.priority)
        assertTrue(result.spokenText.contains("đang bấm còi"))
    }
}
