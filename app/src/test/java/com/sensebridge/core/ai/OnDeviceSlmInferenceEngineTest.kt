package com.sensebridge.core.ai

import android.content.Context
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class OnDeviceSlmInferenceEngineTest {

    private lateinit var mockContext: Context
    private lateinit var mockModelManager: SlmModelManager
    private lateinit var slmEngine: OnDeviceSlmInferenceEngine
    private lateinit var memory: MultimodalSceneMemory

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockModelManager = mockk(relaxed = true)
        slmEngine = OnDeviceSlmInferenceEngine(mockContext, mockModelManager)
        memory = MultimodalSceneMemory(retentionPeriodMs = 5000L)
    }

    @Test
    fun formatMultimodalPrompt_includesObjectsAndSoundsAndOcr() {
        memory.recordObject(
            ObservedObject(
                label = "bus",
                vietnameseLabel = "xe buýt",
                direction = SpatialDirection.LEFT,
                isNear = true
            )
        )
        memory.recordSound(
            ObservedSound(
                label = "horn",
                vietnameseLabel = "tiếng còi xe",
                decibels = 75.0,
                priority = PriorityLevel.WARNING_P1
            )
        )
        memory.recordText("TRẠM XE BUÝT BẾN THÀNH")
        memory.updateAmbientDecibels(72.0)

        val prompt = slmEngine.formatMultimodalPrompt("Tôi đang ở đâu?", memory.getSnapshot())

        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("xe buýt (bên trái)"))
        assertTrue(prompt.contains("tiếng còi xe"))
        assertTrue(prompt.contains("TRẠM XE BUÝT BẾN THÀNH"))
        assertTrue(prompt.contains("72 dB"))
        assertTrue(prompt.contains("Tôi đang ở đâu?"))
        assertTrue(prompt.contains("<end_of_turn>"))
    }

    @Test
    fun isReady_whenNotInitialized_returnsFalse() {
        assertFalse(slmEngine.isReady())
    }

    @Test
    fun slmModelManager_customPath_resolvesCorrectly() {
        val tempFile = File.createTempFile("test_slm", ".bin")
        tempFile.writeText("model_weights")
        tempFile.deleteOnExit()

        val manager = SlmModelManager(mockContext)
        manager.setCustomModelPath(tempFile.absolutePath)

        assertTrue(manager.isModelAvailable())
        assertEquals(tempFile.absolutePath, manager.getModelFile()?.absolutePath)
    }
}
