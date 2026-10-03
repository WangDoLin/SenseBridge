package com.sensebridge.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sensebridge.data.local.entity.SavedPhraseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {

    @Query("SELECT * FROM saved_phrases ORDER BY isFavorite DESC, usageCount DESC, id ASC")
    fun getAllPhrases(): Flow<List<SavedPhraseEntity>>

    @Query("SELECT * FROM saved_phrases WHERE category = :category ORDER BY usageCount DESC")
    fun getPhrasesByCategory(category: String): Flow<List<SavedPhraseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: SavedPhraseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<SavedPhraseEntity>)

    @Update
    suspend fun updatePhrase(phrase: SavedPhraseEntity)

    @Delete
    suspend fun deletePhrase(phrase: SavedPhraseEntity)

    @Query("UPDATE saved_phrases SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("SELECT COUNT(*) FROM saved_phrases")
    suspend fun countPhrases(): Int
}
