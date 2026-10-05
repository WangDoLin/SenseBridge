package com.sensebridge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensebridge.data.local.entity.UserAiSessionEntity

@Dao
interface UserAiSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserAiSessionEntity): Long

    @Query("SELECT * FROM user_ai_sessions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<UserAiSessionEntity>

    @Query("SELECT * FROM user_ai_sessions WHERE userInput LIKE '%' || :keyword || '%' OR sceneText LIKE '%' || :keyword || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun findSessionsByKeyword(keyword: String, limit: Int): List<UserAiSessionEntity>

    @Query("SELECT * FROM user_ai_sessions WHERE sceneText IS NOT NULL AND sceneText = :sceneText ORDER BY timestamp DESC LIMIT :limit")
    suspend fun findSessionsByExactSceneText(sceneText: String, limit: Int): List<UserAiSessionEntity>

    @Query("DELETE FROM user_ai_sessions WHERE timestamp < :olderThan")
    suspend fun deleteSessionsOlderThan(olderThan: Long): Int

    @Query("SELECT COUNT(*) FROM user_ai_sessions")
    suspend fun getTotalSessionCount(): Int
}
