package com.sensebridge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sensebridge.data.local.dao.PhraseDao
import com.sensebridge.data.local.entity.SavedPhraseEntity

@Database(
    entities = [SavedPhraseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SenseBridgeDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
}
