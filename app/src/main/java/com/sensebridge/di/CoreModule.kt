package com.sensebridge.di

import com.sensebridge.core.engine.MultimodalSynthesizer
import com.sensebridge.core.engine.SmartEventDebouncer
import com.sensebridge.output.haptic.AndroidHapticManager
import com.sensebridge.output.haptic.HapticManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindsModule {

    @Binds
    @Singleton
    abstract fun bindHapticManager(
        androidHapticManager: AndroidHapticManager
    ): HapticManager
}

@Module
@InstallIn(SingletonComponent::class)
object CoreProvidesModule {

    @Provides
    @Singleton
    fun provideSmartEventDebouncer(): SmartEventDebouncer {
        return SmartEventDebouncer()
    }

    @Provides
    @Singleton
    fun provideMultimodalSynthesizer(): MultimodalSynthesizer {
        return MultimodalSynthesizer()
    }
}
