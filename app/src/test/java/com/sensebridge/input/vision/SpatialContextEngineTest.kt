package com.sensebridge.input.vision

import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpatialContextEngineTest {

    private lateinit var engine: SpatialContextEngine

    @Before
    fun setUp() {
        engine = SpatialContextEngine()
    }

    @Test
    fun analyze_objectOnLeft_returnsLeftDirection() {
        // Frame 640x480. Box on the left side (x: 50 to 150 -> center 100 / 640 = 0.156)
        val result = engine.analyze(left = 50, top = 100, right = 150, bottom = 300, frameWidth = 640, frameHeight = 480)

        assertEquals(SpatialDirection.LEFT, result.direction)
    }

    @Test
    fun analyze_objectInCenter_returnsCenterDirection() {
        // Frame 640x480. Box in center (x: 270 to 370 -> center 320 / 640 = 0.5)
        val result = engine.analyze(left = 270, top = 100, right = 370, bottom = 300, frameWidth = 640, frameHeight = 480)

        assertEquals(SpatialDirection.CENTER, result.direction)
    }

    @Test
    fun analyze_objectOnRight_returnsRightDirection() {
        // Frame 640x480. Box on right (x: 480 to 580 -> center 530 / 640 = 0.828)
        val result = engine.analyze(left = 480, top = 100, right = 580, bottom = 300, frameWidth = 640, frameHeight = 480)

        assertEquals(SpatialDirection.RIGHT, result.direction)
    }

    @Test
    fun analyze_largeBox_isNearTrue() {
        // Very large box occupying over 35% of the screen
        val result = engine.analyze(left = 100, top = 50, right = 540, bottom = 430, frameWidth = 640, frameHeight = 480)

        assertTrue(result.isNear)
        assertFalse(result.isFar)
    }

    @Test
    fun analyze_tinyBox_isFarTrue() {
        // Tiny box occupying less than 5% of the screen
        val result = engine.analyze(left = 300, top = 200, right = 340, bottom = 240, frameWidth = 640, frameHeight = 480)

        assertTrue(result.isFar)
        assertFalse(result.isNear)
    }
}
