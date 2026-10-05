package com.sensebridge.input.vision

import com.sensebridge.core.engine.EventEngine
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OcrEngineTest {

    private lateinit var ocrEngine: OcrEngine
    private val mockEventEngine: EventEngine = mockk(relaxed = true)

    @Before
    fun setUp() {
        ocrEngine = OcrEngine(mockEventEngine)
    }

    @Test
    fun cleanRecognizedText_multilineClinicSign_formatsCleanly() {
        val rawSign = """
            PHÒNG KHÁM SỐ 3
            .
            KHOA TAI MŨI HỌNG
            1
            TẦNG 2
        """.trimIndent()

        val cleaned = ocrEngine.cleanRecognizedText(rawSign)

        assertTrue(cleaned.contains("PHÒNG KHÁM SỐ 3"))
        assertTrue(cleaned.contains("KHOA TAI MŨI HỌNG"))
        assertTrue(cleaned.contains("TẦNG 2"))
        assertEquals("PHÒNG KHÁM SỐ 3, KHOA TAI MŨI HỌNG, TẦNG 2", cleaned)
    }
}
