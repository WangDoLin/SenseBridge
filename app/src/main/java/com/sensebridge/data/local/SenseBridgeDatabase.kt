package com.sensebridge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sensebridge.data.local.dao.LearnedUserPatternDao
import com.sensebridge.data.local.dao.PhraseDao
import com.sensebridge.data.local.dao.UserAiSessionDao
import com.sensebridge.data.local.entity.LearnedUserPatternEntity
import com.sensebridge.data.local.entity.SavedPhraseEntity
import com.sensebridge.data.local.entity.UserAiSessionEntity

@Database(
    entities = [
        SavedPhraseEntity::class,
        UserAiSessionEntity::class,
        LearnedUserPatternEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SenseBridgeDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
    abstract fun userAiSessionDao(): UserAiSessionDao
    abstract fun learnedUserPatternDao(): LearnedUserPatternDao
}
