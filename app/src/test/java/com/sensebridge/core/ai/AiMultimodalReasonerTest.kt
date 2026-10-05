package com.sensebridge.core.ai

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiMultimodalReasonerTest {

    private lateinit var reasoner: AiMultimodalReasoner
    private lateinit var memory: MultimodalSceneMemory

    @Before
    fun setUp() {
        reasoner = AiMultimodalReasoner()
        memory = MultimodalSceneMemory(retentionPeriodMs = 5000L)
    }

    @Test
    fun evaluateSceneThreat_vehicleAndHornNearby_returnsCriticalP0() {
        memory.recordObject(
            ObservedObject(
                label = "car",
                vietnameseLabel = "xe ô tô",
                direction = SpatialDirection.RIGHT,
                isNear = true
            )
        )
        memory.recordSound(
            ObservedSound(
                label = "car_horn",
                vietnameseLabel = "tiếng còi xe",
                decibels = 82.0,
                priority = PriorityLevel.WARNING_P1
            )
        )

        val insight = reasoner.evaluateSceneThreat(memory.getSnapshot())

        assertNotNull(insight)
        assertEquals(PriorityLevel.CRITICAL_P0, insight?.priority)
        assertTrue(insight?.spokenText?.contains("bên phải") == true)
        assertTrue(insight?.spokenText?.contains("rất gần") == true)
    }

    @Test
    fun evaluateSceneThreat_sirenSound_returnsEmergencyP0() {
        memory.recordSound(
            ObservedSound(
                label = "ambulance_siren",
                vietnameseLabel = "còi xe cứu thương",
                decibels = 78.0,
                priority = PriorityLevel.CRITICAL_P0
            )
        )

        val insight = reasoner.evaluateSceneThreat(memory.getSnapshot())

        assertNotNull(insight)
        assertEquals(PriorityLevel.CRITICAL_P0, insight?.priority)
        assertEquals("Có còi ưu tiên", insight?.title)
    }

    @Test
    fun evaluateSceneThreat_stairsAhead_returnsWarningP1() {
        memory.recordObject(
            ObservedObject(
                label = "stairs",
                vietnameseLabel = "bậc thang",
                direction = SpatialDirection.CENTER,
                isNear = true
            )
        )

        val insight = reasoner.evaluateSceneThreat(memory.getSnapshot())

        assertNotNull(insight)
        assertEquals(PriorityLevel.WARNING_P1, insight?.priority)
        assertTrue(insight?.spokenText?.contains("bậc thang") == true)
    }

    @Test
    fun evaluateSceneThreat_distressSound_returnsWarningP1() {
        memory.recordSound(
            ObservedSound(
                label = "screaming",
                vietnameseLabel = "tiếng la hét",
                decibels = 74.0,
                priority = PriorityLevel.WARNING_P1
            )
        )

        val insight = reasoner.evaluateSceneThreat(memory.getSnapshot())

        assertNotNull(insight)
        assertEquals(PriorityLevel.WARNING_P1, insight?.priority)
    }

    @Test
    fun evaluateSceneThreat_importantSignage_returnsAttentionP2() {
        memory.recordText("KHOA CẤP CỨU")

        val insight = reasoner.evaluateSceneThreat(memory.getSnapshot())

        assertNotNull(insight)
        assertEquals(PriorityLevel.ATTENTION_P2, insight?.priority)
        assertTrue(insight?.spokenText?.contains("KHOA CẤP CỨU") == true)
    }

    @Test
    fun evaluateSceneThreat_calmEnvironment_returnsNull() {
        memory.recordObject(
            ObservedObject(
                label = "chair",
                vietnameseLabel = "ghế",
                direction = SpatialDirection.LEFT,
                isNear = false
            )
        )

        val insight = reasoner.evaluateSceneThreat(memory.getSnapshot())
        assertNull(insight)
    }

    @Test
    fun answerUserQuery_safetyQueryWithVehicle_returnsWarning() {
        memory.recordObject(
            ObservedObject(
                label = "bus",
                vietnameseLabel = "xe buýt",
                direction = SpatialDirection.CENTER,
                isNear = true
            )
        )

        val answer = reasoner.answerUserQuery("Có qua đường an toàn không?", memory.getSnapshot())

        assertEquals(PriorityLevel.WARNING_P1, answer.priority)
        assertTrue(answer.answerText.contains("Chưa an toàn"))
        assertTrue(answer.answerText.contains("xe buýt"))
    }

    @Test
    fun answerUserQuery_safetyQueryCalm_returnsSafe() {
        val answer = reasoner.answerUserQuery("Tôi có an toàn không?", memory.getSnapshot())

        assertEquals(PriorityLevel.INFO_P3, answer.priority)
        assertTrue(answer.answerText.contains("tương đối an toàn"))
    }

    @Test
    fun answerUserQuery_surroundingsQuery_describesObjectsAndSound() {
        memory.recordObject(
            ObservedObject(
                label = "person",
                vietnameseLabel = "người",
                direction = SpatialDirection.CENTER,
                isNear = true
            )
        )
        memory.recordSound(
            ObservedSound(
                label = "footsteps",
                vietnameseLabel = "tiếng bước chân",
                decibels = 55.0,
                priority = PriorityLevel.INFO_P3
            )
        )

        val answer = reasoner.answerUserQuery("Xung quanh có gì?", memory.getSnapshot())

        assertEquals(PriorityLevel.ATTENTION_P2, answer.priority)
        assertTrue(answer.answerText.contains("người"))
        assertTrue(answer.answerText.contains("tiếng bước chân"))
    }

    @Test
    fun answerUserQuery_readTextQuery_returnsOcrText() {
        memory.recordText("LỐI THOÁT HIỂM")

        val answer = reasoner.answerUserQuery("Đọc biển báo phía trước", memory.getSnapshot())

        assertEquals(PriorityLevel.ATTENTION_P2, answer.priority)
        assertTrue(answer.answerText.contains("LỐI THOÁT HIỂM"))
    }

    @Test
    fun answerUserQuery_soundQuery_reportsSoundsAndDecibels() {
        memory.updateAmbientDecibels(68.5)
        memory.recordSound(
            ObservedSound(
                label = "dog_bark",
                vietnameseLabel = "tiếng chó sủa",
                decibels = 72.0,
                priority = PriorityLevel.ATTENTION_P2
            )
        )

        val answer = reasoner.answerUserQuery("Có tiếng gì thế?", memory.getSnapshot())

        assertEquals(PriorityLevel.ATTENTION_P2, answer.priority)
        assertTrue(answer.answerText.contains("tiếng chó sủa"))
        assertTrue(answer.answerText.contains("68"))
    }

    @Test
    fun sceneMemory_retentionExpiry_removesOldEntries() {
        val past = System.currentTimeMillis() - 10000L
        memory.recordObject(
            ObservedObject(
                label = "car",
                vietnameseLabel = "xe ô tô",
                direction = SpatialDirection.CENTER,
                isNear = true,
                timestamp = past
            )
        )

        val snapshot = memory.getSnapshot()
        assertTrue(snapshot.objects.isEmpty())
    }
}
