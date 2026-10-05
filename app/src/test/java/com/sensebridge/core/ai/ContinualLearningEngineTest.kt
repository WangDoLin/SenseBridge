package com.sensebridge.core.ai

import com.sensebridge.data.local.dao.LearnedUserPatternDao
import com.sensebridge.data.local.dao.UserAiSessionDao
import com.sensebridge.data.local.entity.LearnedUserPatternEntity
import com.sensebridge.data.local.entity.UserAiSessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContinualLearningEngineTest {

    private lateinit var mockSessionDao: UserAiSessionDao
    private lateinit var mockPatternDao: LearnedUserPatternDao
    private lateinit var learningEngine: ContinualLearningEngine
    private lateinit var memory: MultimodalSceneMemory

    @Before
    fun setUp() {
        mockSessionDao = mockk(relaxed = true)
        mockPatternDao = mockk(relaxed = true)
        learningEngine = ContinualLearningEngine(mockSessionDao, mockPatternDao)
        memory = MultimodalSceneMemory(retentionPeriodMs = 5000L)
    }

    @Test
    fun recordAndLearnSession_insertsSessionAndPattern() = runBlocking {
        coEvery { mockPatternDao.findPatternByPhrase(any()) } returns null
        coEvery { mockSessionDao.insertSession(any()) } returns 1L
        coEvery { mockPatternDao.upsertPattern(any()) } returns 1L

        learningEngine.recordAndLearnSession(
            userInput = "Bước tiếp coi",
            aiResponse = "Khu vực an toàn.",
            intent = DialogueIntent.SAFETY_CHECK,
            situation = SituationState.SAFE_QUIET,
            snapshot = memory.getSnapshot()
        )

        coVerify { mockSessionDao.insertSession(match { it.userInput == "Bước tiếp coi" }) }
        coVerify { mockPatternDao.upsertPattern(match { it.userPhrase == "bước tiếp coi" && it.mappedIntent == "SAFETY_CHECK" }) }
    }

    @Test
    fun predictLearnedIntent_whenPatternExists_returnsPersonalizedIntent() = runBlocking {
        coEvery { mockPatternDao.findPatternByPhrase("bước tiếp coi") } returns LearnedUserPatternEntity(
            userPhrase = "bước tiếp coi",
            mappedIntent = "SAFETY_CHECK",
            frequency = 3,
            confidenceWeight = 0.85f
        )

        val prediction = learningEngine.predictLearnedIntent("Bước tiếp coi")

        assertNotNull(prediction)
        assertEquals(DialogueIntent.SAFETY_CHECK, prediction?.intent)
        assertEquals(0.85f, prediction?.confidence ?: 0f, 0.01f)
    }

    @Test
    fun retrieveEpisodicMemory_whenExactSceneTextMatches_returnsLocationMemory() = runBlocking {
        memory.recordText("CỬA HÀNG TIỆN LỢI CIRCLE K")
        coEvery { mockSessionDao.findSessionsByExactSceneText("CỬA HÀNG TIỆN LỢI CIRCLE K", 1) } returns listOf(
            UserAiSessionEntity(
                userInput = "Đây là đâu",
                aiResponse = "Circle K",
                intent = "SITUATION_INQUIRY",
                situation = "READING_SIGNAGE",
                sceneText = "CỬA HÀNG TIỆN LỢI CIRCLE K"
            )
        )

        val memoryContext = learningEngine.retrieveEpisodicMemory("Đây là đâu?", memory.getSnapshot())

        assertNotNull(memoryContext)
        assertTrue(memoryContext!!.contains("CIRCLE K"))
    }

    @Test
    fun converseAsync_integratesLearnedPatternAndMemory() = runBlocking {
        coEvery { mockPatternDao.findPatternByPhrase("bước tiếp coi") } returns LearnedUserPatternEntity(
            userPhrase = "bước tiếp coi",
            mappedIntent = "SAFETY_CHECK",
            frequency = 4,
            confidenceWeight = 0.90f
        )
        coEvery { mockSessionDao.findSessionsByExactSceneText(any(), any()) } returns emptyList()
        coEvery { mockSessionDao.findSessionsByKeyword(any(), any()) } returns emptyList()

        val model = SenseAiDialogueModel(continualLearningEngine = learningEngine)
        val response = model.converseAsync("Bước tiếp coi", memory.getSnapshot())

        assertEquals(DialogueIntent.SAFETY_CHECK, response.intent)
        coVerify { mockSessionDao.insertSession(any()) }
    }
}
