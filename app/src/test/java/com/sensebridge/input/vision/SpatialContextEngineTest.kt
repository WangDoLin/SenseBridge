package com.sensebridge.input.vision

import android.graphics.Rect
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
        val leftBox = Rect(50, 100, 150, 300)
        val result = engine.analyze(leftBox, 640, 480)

        assertEquals(SpatialDirection.LEFT, result.direction)
    }

    @Test
    fun analyze_objectInCenter_returnsCenterDirection() {
        // Frame 640x480. Box in center (x: 270 to 370 -> center 320 / 640 = 0.5)
        val centerBox = Rect(270, 100, 370, 300)
        val result = engine.analyze(centerBox, 640, 480)

        assertEquals(SpatialDirection.CENTER, result.direction)
    }

    @Test
    fun analyze_objectOnRight_returnsRightDirection() {
        // Frame 640x480. Box on right (x: 480 to 580 -> center 530 / 640 = 0.828)
        val rightBox = Rect(480, 100, 580, 300)
        val result = engine.analyze(rightBox, 640, 480)

        assertEquals(SpatialDirection.RIGHT, result.direction)
    }

    @Test
    fun analyze_largeBox_isNearTrue() {
        // Very large box occupying over 35% of the screen
        val largeBox = Rect(100, 50, 540, 430)
        val result = engine.analyze(largeBox, 640, 480)

        assertTrue(result.isNear)
        assertFalse(result.isFar)
    }

    @Test
    fun analyze_tinyBox_isFarTrue() {
        // Tiny box occupying less than 5% of the screen
        val tinyBox = Rect(300, 200, 340, 240)
        val result = engine.analyze(tinyBox, 640, 480)

        assertTrue(result.isFar)
        assertFalse(result.isNear)
    }
}
