package com.sensebridge.di

import android.content.Context
import androidx.room.Room
import com.sensebridge.data.local.SenseBridgeDatabase
import com.sensebridge.data.local.dao.PhraseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSenseBridgeDatabase(
        @ApplicationContext context: Context
    ): SenseBridgeDatabase {
        return Room.databaseBuilder(
            context,
            SenseBridgeDatabase::class.java,
            "sensebridge.db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePhraseDao(database: SenseBridgeDatabase): PhraseDao {
        return database.phraseDao()
    }
}
