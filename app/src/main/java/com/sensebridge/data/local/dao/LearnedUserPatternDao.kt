package com.sensebridge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensebridge.data.local.entity.LearnedUserPatternEntity

@Dao
interface LearnedUserPatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPattern(pattern: LearnedUserPatternEntity): Long

    @Query("SELECT * FROM learned_user_patterns WHERE userPhrase = :phrase LIMIT 1")
    suspend fun findPatternByPhrase(phrase: String): LearnedUserPatternEntity?

    @Query("SELECT * FROM learned_user_patterns ORDER BY frequency DESC, confidenceWeight DESC LIMIT :limit")
    suspend fun getTopLearnedPatterns(limit: Int): List<LearnedUserPatternEntity>

    @Query("SELECT * FROM learned_user_patterns")
    suspend fun getAllLearnedPatterns(): List<LearnedUserPatternEntity>

    @Query("UPDATE learned_user_patterns SET frequency = frequency + 1, confidenceWeight = MIN(1.0, confidenceWeight + :weightIncrement), lastUsedTimestamp = :timestamp WHERE userPhrase = :phrase")
    suspend fun incrementPattern(phrase: String, weightIncrement: Float, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM learned_user_patterns")
    suspend fun getTotalPatternCount(): Int
}
