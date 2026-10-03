package com.sensebridge.input.sound

import android.content.Context
import com.sensebridge.core.model.PriorityLevel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TFLiteAudioClassifierTest {

    private lateinit var classifier: TFLiteAudioClassifier
    private val mockContext: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        classifier = TFLiteAudioClassifier(mockContext)
    }

    @Test
    fun mapLabelToResult_carHorn_mapsToCriticalP0() {
        val result = classifier.mapLabelToResult("Vehicle horn, car horn, honking", 0.88f)

        assertNotNull(result)
        assertEquals("car_horn", result?.label)
        assertEquals(PriorityLevel.CRITICAL_P0, result?.priority)
        assertTrue(result?.spokenText?.contains("còi xe") == true)
    }

    @Test
    fun mapLabelToResult_fireAlarm_mapsToCriticalP0() {
        val result = classifier.mapLabelToResult("Smoke detector, smoke alarm, fire alarm", 0.94f)

        assertNotNull(result)
        assertEquals("fire_alarm", result?.label)
        assertEquals(PriorityLevel.CRITICAL_P0, result?.priority)
        assertTrue(result?.spokenText?.contains("báo cháy") == true)
    }

    @Test
    fun mapLabelToResult_doorbell_mapsToAttentionP2() {
        val result = classifier.mapLabelToResult("Doorbell, ding-dong", 0.85f)

        assertNotNull(result)
        assertEquals("doorbell", result?.label)
        assertEquals(PriorityLevel.ATTENTION_P2, result?.priority)
    }

    @Test
    fun mapLabelToResult_babyCry_mapsToWarningP1() {
        val result = classifier.mapLabelToResult("Crying, sobbing, baby cry", 0.79f)

        assertNotNull(result)
        assertEquals("baby_cry", result?.label)
        assertEquals(PriorityLevel.WARNING_P1, result?.priority)
    }

    @Test
    fun mapLabelToResult_unimportantNoise_returnsNull() {
        val result = classifier.mapLabelToResult("Air conditioner humming", 0.90f)

        assertNull(result)
    }
}
