package com.sensebridge.data.repository

import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.data.local.dao.PhraseDao
import com.sensebridge.data.local.entity.SavedPhraseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhraseRepository @Inject constructor(
    private val phraseDao: PhraseDao,
    private val eventEngine: EventEngine
) {
    val allPhrases: Flow<List<SavedPhraseEntity>> = phraseDao.getAllPhrases()

    suspend fun initializeDefaultPhrasesIfNeeded() {
        if (phraseDao.countPhrases() == 0) {
            val defaults = listOf(
                SavedPhraseEntity(
                    vietnameseText = "Tôi cần giúp đỡ khẩn cấp!",
                    category = "emergency",
                    isFavorite = true,
                    priority = PriorityLevel.CRITICAL_P0
                ),
                SavedPhraseEntity(
                    vietnameseText = "Tôi bị đau dữ dội, hãy giúp tôi!",
                    category = "medical",
                    isFavorite = true,
                    priority = PriorityLevel.WARNING_P1
                ),
                SavedPhraseEntity(
                    vietnameseText = "Làm ơn gọi người thân giúp tôi.",
                    category = "emergency",
                    isFavorite = true,
                    priority = PriorityLevel.WARNING_P1
                ),
                SavedPhraseEntity(
                    vietnameseText = "Tôi cần đến bệnh viện gần nhất.",
                    category = "medical",
                    isFavorite = true,
                    priority = PriorityLevel.WARNING_P1
                ),
                SavedPhraseEntity(
                    vietnameseText = "Tôi cần uống nước.",
                    category = "daily",
                    isFavorite = false,
                    priority = PriorityLevel.ATTENTION_P2
                ),
                SavedPhraseEntity(
                    vietnameseText = "Tôi không thể nói được, xin vui lòng đọc chữ trên màn hình.",
                    category = "daily",
                    isFavorite = false,
                    priority = PriorityLevel.ATTENTION_P2
                )
            )
            phraseDao.insertAll(defaults)
        }
    }

    suspend fun speakPhrase(phrase: SavedPhraseEntity) {
        phraseDao.incrementUsage(phrase.id)

        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.COMMUNICATION_INPUT,
                label = "aac_phrase_${phrase.id}",
                displayTitle = phrase.vietnameseText,
                spokenText = phrase.vietnameseText,
                confidence = 1.0f,
                priority = phrase.priority
            )
        )
    }

    suspend fun addCustomPhrase(text: String, isEmergency: Boolean = false) {
        val newPhrase = SavedPhraseEntity(
            vietnameseText = text.trim(),
            category = if (isEmergency) "emergency" else "custom",
            isFavorite = isEmergency,
            priority = if (isEmergency) PriorityLevel.WARNING_P1 else PriorityLevel.ATTENTION_P2
        )
        phraseDao.insertPhrase(newPhrase)
    }

    suspend fun deletePhrase(phrase: SavedPhraseEntity) {
        phraseDao.deletePhrase(phrase)
    }
}
