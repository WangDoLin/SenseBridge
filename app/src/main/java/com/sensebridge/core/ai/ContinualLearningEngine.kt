package com.sensebridge.core.ai

import com.sensebridge.data.local.dao.LearnedUserPatternDao
import com.sensebridge.data.local.dao.UserAiSessionDao
import com.sensebridge.data.local.entity.LearnedUserPatternEntity
import com.sensebridge.data.local.entity.UserAiSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class LearnedIntentPrediction(
    val intent: DialogueIntent,
    val confidence: Float,
    val userPhrase: String
)

@Singleton
class ContinualLearningEngine @Inject constructor(
    private val sessionDao: UserAiSessionDao,
    private val patternDao: LearnedUserPatternDao
) {
    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.50f
        private const val WEIGHT_INCREMENT = 0.10f
    }

    suspend fun recordAndLearnSession(
        userInput: String,
        aiResponse: String,
        intent: DialogueIntent,
        situation: SituationState,
        snapshot: SceneSnapshot
    ) = withContext(Dispatchers.IO) {
        val cleanInput = cleanPhrase(userInput)
        if (cleanInput.isBlank()) return@withContext

        val session = UserAiSessionEntity(
            userInput = userInput.trim(),
            aiResponse = aiResponse.trim(),
            intent = intent.name,
            situation = situation.name,
            sceneText = snapshot.latestText,
            sceneObjects = snapshot.objects.take(3).joinToString(",") { it.vietnameseLabel },
            sceneSounds = snapshot.sounds.take(2).joinToString(",") { it.vietnameseLabel },
            ambientDecibels = snapshot.ambientDecibels,
            timestamp = System.currentTimeMillis()
        )
        sessionDao.insertSession(session)

        learnUserPattern(cleanInput, intent)
    }

    suspend fun predictLearnedIntent(userInput: String): LearnedIntentPrediction? = withContext(Dispatchers.IO) {
        val cleanInput = cleanPhrase(userInput)
        if (cleanInput.isBlank()) return@withContext null

        val exactMatch = patternDao.findPatternByPhrase(cleanInput)
        if (exactMatch != null && exactMatch.confidenceWeight >= MIN_CONFIDENCE_THRESHOLD) {
            val intent = try {
                DialogueIntent.valueOf(exactMatch.mappedIntent)
            } catch (e: Exception) {
                DialogueIntent.GENERAL
            }
            return@withContext LearnedIntentPrediction(
                intent = intent,
                confidence = exactMatch.confidenceWeight,
                userPhrase = exactMatch.userPhrase
            )
        }

        val topPatterns = patternDao.getTopLearnedPatterns(limit = 30)
        val containedMatch = topPatterns.firstOrNull { pattern ->
            cleanInput.contains(pattern.userPhrase) && pattern.confidenceWeight >= MIN_CONFIDENCE_THRESHOLD
        }

        if (containedMatch != null) {
            val intent = try {
                DialogueIntent.valueOf(containedMatch.mappedIntent)
            } catch (e: Exception) {
                DialogueIntent.GENERAL
            }
            return@withContext LearnedIntentPrediction(
                intent = intent,
                confidence = containedMatch.confidenceWeight,
                userPhrase = containedMatch.userPhrase
            )
        }

        null
    }

    suspend fun retrieveEpisodicMemory(
        userInput: String,
        snapshot: SceneSnapshot
    ): String? = withContext(Dispatchers.IO) {
        val currentText = snapshot.latestText?.trim()
        if (!currentText.isNullOrBlank()) {
            val pastSessionsWithText = sessionDao.findSessionsByExactSceneText(currentText, limit = 1)
            val past = pastSessionsWithText.firstOrNull()
            if (past != null) {
                return@withContext "Ký ức địa điểm: Bạn từng ở đây khi camera đọc thấy \"$currentText\"."
            }
        }

        val cleanKeyword = cleanPhrase(userInput)
        if (cleanKeyword.length >= 4) {
            val pastMatches = sessionDao.findSessionsByKeyword(cleanKeyword, limit = 1)
            val past = pastMatches.firstOrNull()
            if (past != null && past.userInput != userInput) {
                return@withContext "Ghi nhớ trước đây: Bạn từng quan tâm về \"${past.userInput}\"."
            }
        }

        null
    }

    private suspend fun learnUserPattern(cleanPhrase: String, intent: DialogueIntent) {
        val existing = patternDao.findPatternByPhrase(cleanPhrase)
        val now = System.currentTimeMillis()
        if (existing != null) {
            patternDao.incrementPattern(cleanPhrase, WEIGHT_INCREMENT, now)
        } else {
            val newPattern = LearnedUserPatternEntity(
                userPhrase = cleanPhrase,
                mappedIntent = intent.name,
                frequency = 1,
                confidenceWeight = 0.60f,
                lastUsedTimestamp = now
            )
            patternDao.upsertPattern(newPattern)
        }
    }

    private fun cleanPhrase(input: String): String {
        return input.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }
}
