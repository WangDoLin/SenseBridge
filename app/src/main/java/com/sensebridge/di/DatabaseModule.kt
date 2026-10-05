package com.sensebridge.di

import android.content.Context
import androidx.room.Room
import com.sensebridge.data.local.SenseBridgeDatabase
import com.sensebridge.data.local.dao.LearnedUserPatternDao
import com.sensebridge.data.local.dao.PhraseDao
import com.sensebridge.data.local.dao.UserAiSessionDao
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
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun providePhraseDao(database: SenseBridgeDatabase): PhraseDao {
        return database.phraseDao()
    }

    @Provides
    @Singleton
    fun provideUserAiSessionDao(database: SenseBridgeDatabase): UserAiSessionDao {
        return database.userAiSessionDao()
    }

    @Provides
    @Singleton
    fun provideLearnedUserPatternDao(database: SenseBridgeDatabase): LearnedUserPatternDao {
        return database.learnedUserPatternDao()
    }
}
