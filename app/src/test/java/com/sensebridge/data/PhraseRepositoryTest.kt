package com.sensebridge.data

import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.data.local.dao.PhraseDao
import com.sensebridge.data.local.entity.SavedPhraseEntity
import com.sensebridge.data.repository.PhraseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PhraseRepositoryTest {

    private val mockDao: PhraseDao = mockk(relaxed = true)
    private val mockEventEngine: EventEngine = mockk(relaxed = true)
    private lateinit var repository: PhraseRepository

    @Before
    fun setUp() {
        repository = PhraseRepository(mockDao, mockEventEngine)
    }

    @Test
    fun initializeDefaultPhrasesIfNeeded_whenEmpty_insertsDefaults() = runTest {
        coEvery { mockDao.countPhrases() } returns 0

        repository.initializeDefaultPhrasesIfNeeded()

        coVerify { mockDao.insertAll(match { it.size >= 5 }) }
    }

    @Test
    fun speakPhrase_submitsEventToEngineAndIncrementsCount() = runTest {
        val phrase = SavedPhraseEntity(
            id = 1L,
            vietnameseText = "Tôi cần giúp đỡ khẩn cấp!",
            category = "emergency",
            priority = PriorityLevel.CRITICAL_P0
        )

        repository.speakPhrase(phrase)

        coVerify { mockDao.incrementUsage(1L) }
        coVerify { mockEventEngine.submitEvent(match {
            it.spokenText == "Tôi cần giúp đỡ khẩn cấp!" && it.priority == PriorityLevel.CRITICAL_P0
        }) }
    }

    @Test
    fun addCustomPhrase_insertsProperEntity() = runTest {
        val slot = slot<SavedPhraseEntity>()
        coEvery { mockDao.insertPhrase(capture(slot)) } returns 10L

        repository.addCustomPhrase("Xin chào, tôi cần mua bánh mì")

        assertEquals("Xin chào, tôi cần mua bánh mì", slot.captured.vietnameseText)
        assertEquals("custom", slot.captured.category)
        assertEquals(PriorityLevel.ATTENTION_P2, slot.captured.priority)
    }
}
