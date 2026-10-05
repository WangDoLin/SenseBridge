package com.sensebridge.core.ai

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SenseAiDialogueModelTest {

    private lateinit var dialogueModel: SenseAiDialogueModel
    private lateinit var memory: MultimodalSceneMemory

    @Before
    fun setUp() {
        dialogueModel = SenseAiDialogueModel()
        memory = MultimodalSceneMemory(retentionPeriodMs = 5000L)
    }

    @Test
    fun classifySituation_whenSirenPresent_classifiesEmergencyHazard() {
        memory.recordSound(
            ObservedSound(
                label = "siren",
                vietnameseLabel = "tiếng còi cứu thương",
                decibels = 80.0,
                priority = PriorityLevel.CRITICAL_P0
            )
        )

        val result = dialogueModel.classifySituation(memory.getSnapshot())

        assertEquals(SituationState.EMERGENCY_HAZARD, result.state)
        assertEquals(PriorityLevel.CRITICAL_P0, result.priority)
        assertTrue(result.summary.contains("nguy hiểm"))
    }

    @Test
    fun classifySituation_whenVehiclePresent_classifiesStreetTraffic() {
        memory.recordObject(
            ObservedObject(
                label = "car",
                vietnameseLabel = "xe ô tô",
                direction = SpatialDirection.RIGHT,
                isNear = true
            )
        )

        val result = dialogueModel.classifySituation(memory.getSnapshot())

        assertEquals(SituationState.STREET_TRAFFIC, result.state)
        assertEquals(PriorityLevel.WARNING_P1, result.priority)
        assertTrue(result.summary.contains("giao thông"))
    }

    @Test
    fun classifySituation_whenTextDetected_classifiesReadingSignage() {
        memory.recordText("PHÒNG CẤP CỨU SỐ 2")

        val result = dialogueModel.classifySituation(memory.getSnapshot())

        assertEquals(SituationState.READING_SIGNAGE, result.state)
        assertEquals(PriorityLevel.ATTENTION_P2, result.priority)
    }

    @Test
    fun classifySituation_whenPersonAndSpeech_classifiesSocialMeeting() {
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
                label = "speech",
                vietnameseLabel = "tiếng nói",
                decibels = 62.0,
                priority = PriorityLevel.INFO_P3
            )
        )

        val result = dialogueModel.classifySituation(memory.getSnapshot())

        assertEquals(SituationState.SOCIAL_MEETING, result.state)
        assertEquals(PriorityLevel.ATTENTION_P2, result.priority)
    }

    @Test
    fun classifySituation_whenFurniture_classifiesIndoorNavigation() {
        memory.recordObject(
            ObservedObject(
                label = "chair",
                vietnameseLabel = "ghế",
                direction = SpatialDirection.LEFT,
                isNear = false
            )
        )

        val result = dialogueModel.classifySituation(memory.getSnapshot())

        assertEquals(SituationState.INDOOR_NAVIGATION, result.state)
        assertEquals(PriorityLevel.INFO_P3, result.priority)
    }

    @Test
    fun classifySituation_whenEmpty_classifiesSafeQuiet() {
        val result = dialogueModel.classifySituation(memory.getSnapshot())

        assertEquals(SituationState.SAFE_QUIET, result.state)
        assertEquals(PriorityLevel.INFO_P3, result.priority)
        assertTrue(result.summary.contains("yên tĩnh và an toàn"))
    }

    @Test
    fun converse_greeting_repliesFriendlyWithSituation() {
        memory.recordObject(
            ObservedObject(
                label = "table",
                vietnameseLabel = "bàn",
                direction = SpatialDirection.CENTER,
                isNear = false
            )
        )

        val response = dialogueModel.converse("Xin chào AI", memory.getSnapshot())

        assertEquals(DialogueIntent.GREETING, response.intent)
        assertTrue(response.replyText.contains("Xin chào bạn! Mình là AI SenseBridge"))
    }

    @Test
    fun converse_safetyCheck_inTraffic_givesWarningAdvice() {
        memory.recordObject(
            ObservedObject(
                label = "motorcycle",
                vietnameseLabel = "xe máy",
                direction = SpatialDirection.LEFT,
                isNear = true
            )
        )

        val response = dialogueModel.converse("Có qua đường an toàn không bạn?", memory.getSnapshot())

        assertEquals(DialogueIntent.SAFETY_CHECK, response.intent)
        assertEquals(PriorityLevel.WARNING_P1, response.priority)
        assertTrue(response.replyText.contains("Chưa an toàn"))
        assertTrue(response.replyText.contains("bên trái"))
    }

    @Test
    fun converse_safetyCheck_whenSafe_givesReassurance() {
        val response = dialogueModel.converse("Đi tiếp được không?", memory.getSnapshot())

        assertEquals(DialogueIntent.SAFETY_CHECK, response.intent)
        assertTrue(response.replyText.contains("rất an toàn"))
    }

    @Test
    fun converse_surroundings_listsObjectsAndSounds() {
        memory.recordObject(
            ObservedObject(
                label = "person",
                vietnameseLabel = "người",
                direction = SpatialDirection.CENTER,
                isNear = true
            )
        )
        memory.updateAmbientDecibels(58.0)

        val response = dialogueModel.converse("Xung quanh có gì vậy?", memory.getSnapshot())

        assertEquals(DialogueIntent.SURROUNDINGS_OBSERVE, response.intent)
        assertTrue(response.replyText.contains("người"))
        assertTrue(response.replyText.contains("58"))
    }

    @Test
    fun converse_readText_quotesLatestText() {
        memory.recordText("LỐI RA XE BUÝT")

        val response = dialogueModel.converse("Đọc biển báo giúp tôi", memory.getSnapshot())

        assertEquals(DialogueIntent.READ_TEXT, response.intent)
        assertTrue(response.replyText.contains("LỐI RA XE BUÝT"))
    }

    @Test
    fun converse_helpRequest_activatesEmergencyAdvice() {
        val response = dialogueModel.converse("Giúp tôi với!", memory.getSnapshot())

        assertEquals(DialogueIntent.HELP_REQUEST, response.intent)
        assertEquals(PriorityLevel.CRITICAL_P0, response.priority)
        assertTrue(response.replyText.contains("hỗ trợ khẩn cấp"))
    }

    @Test
    fun converse_smalltalk_answersIdentityAndGratitude() {
        val identityResponse = dialogueModel.converse("Bạn tên là gì?", memory.getSnapshot())
        assertEquals(DialogueIntent.SMALLTALK, identityResponse.intent)
        assertTrue(identityResponse.replyText.contains("SenseAI"))

        val thanksResponse = dialogueModel.converse("Cảm ơn bạn nhiều", memory.getSnapshot())
        assertEquals(DialogueIntent.SMALLTALK, thanksResponse.intent)
        assertTrue(thanksResponse.replyText.contains("Rất vui được hỗ trợ"))
    }

    @Test
    fun converse_followUp_remembersPreviousContext() {
        dialogueModel.converse("Có an toàn không?", memory.getSnapshot())

        val followUp = dialogueModel.converse("Còn bây giờ thì sao?", memory.getSnapshot())
        assertEquals(DialogueIntent.FOLLOW_UP, followUp.intent)
        assertTrue(followUp.replyText.contains("Cập nhật tiếp theo"))
    }

    @Test
    fun converseAsync_whenSlmReady_usesGeneratedResponse() = kotlinx.coroutines.runBlocking {
        val mockSlm = io.mockk.mockk<OnDeviceSlmInferenceEngine>()
        io.mockk.every { mockSlm.isReady() } returns true
        io.mockk.coEvery { mockSlm.generateResponse(any(), any()) } returns "Chào bạn, tôi là mô hình SLM tự do."

        val hybridModel = SenseAiDialogueModel(slmEngine = mockSlm)
        val response = hybridModel.converseAsync("Xin chào", memory.getSnapshot())

        assertEquals("Chào bạn, tôi là mô hình SLM tự do.", response.replyText)
        assertEquals(DialogueIntent.GREETING, response.intent)
    }

    @Test
    fun converseAsync_whenSlmNotReady_fallsBackToCognitiveReply() = kotlinx.coroutines.runBlocking {
        val mockSlm = io.mockk.mockk<OnDeviceSlmInferenceEngine>()
        io.mockk.every { mockSlm.isReady() } returns false

        val hybridModel = SenseAiDialogueModel(slmEngine = mockSlm)
        val response = hybridModel.converseAsync("Xin chào", memory.getSnapshot())

        assertTrue(response.replyText.contains("Xin chào bạn! Mình là AI SenseBridge"))
    }
}
