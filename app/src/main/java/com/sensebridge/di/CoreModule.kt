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

import com.sensebridge.input.sound.DecibelEnergyGate
import com.sensebridge.input.sound.SoundClassifier
import com.sensebridge.input.sound.TFLiteAudioClassifier

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindsModule {

    @Binds
    @Singleton
    abstract fun bindHapticManager(
        androidHapticManager: AndroidHapticManager
    ): HapticManager

    @Binds
    @Singleton
    abstract fun bindSoundClassifier(
        tfLiteAudioClassifier: TFLiteAudioClassifier
    ): SoundClassifier
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

    @Provides
    @Singleton
    fun provideDecibelEnergyGate(): DecibelEnergyGate {
        return DecibelEnergyGate()
    }

    @Provides
    @Singleton
    fun provideFrameRateThrottle(): com.sensebridge.input.vision.FrameRateThrottle {
        return com.sensebridge.input.vision.FrameRateThrottle(targetFps = 10)
    }

    @Provides
    @Singleton
    fun provideSpatialContextEngine(): com.sensebridge.input.vision.SpatialContextEngine {
        return com.sensebridge.input.vision.SpatialContextEngine()
    }

    @Provides
    @Singleton
    fun provideSentenceGenerator(): com.sensebridge.input.vision.SentenceGenerator {
        return com.sensebridge.input.vision.SentenceGenerator()
    }
}


