package com.sensebridge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sensebridge.core.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Immutable snapshot of user preferences.
 */
data class UserPreferences(
    val enableHaptic: Boolean = true,
    val enableVoice: Boolean = true,
    val enableVisual: Boolean = true,
    val thresholdDb: Double = 58.0,
    val isBackgroundMonitoringEnabled: Boolean = true,
    val voicePitch: Float = 1.0f,
    val voiceSpeechRate: Float = 1.0f,
    val voicePersona: String = "DEFAULT",
    val userName: String = "Bạn",
    val userPronoun: String = "bạn",
    val aiName: String = "SenseBridge",
    val aiPronoun: String = "tôi",
    val isOnboardingCompleted: Boolean = false
) {
    fun toUserProfile(): UserProfile = UserProfile(
        userName = userName,
        userPronoun = userPronoun,
        aiName = aiName,
        aiPronoun = aiPronoun,
        isOnboardingCompleted = isOnboardingCompleted
    )
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sensebridge_preferences")

/**
 * Manages persistent user preferences using Jetpack DataStore Preferences.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_ENABLE_HAPTIC = booleanPreferencesKey("enable_haptic")
        val KEY_ENABLE_VOICE = booleanPreferencesKey("enable_voice")
        val KEY_ENABLE_VISUAL = booleanPreferencesKey("enable_visual")
        val KEY_THRESHOLD_DB = doublePreferencesKey("threshold_db")
        val KEY_BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_VOICE_SPEECH_RATE = floatPreferencesKey("voice_speech_rate")
        val KEY_VOICE_PERSONA = stringPreferencesKey("voice_persona")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_PRONOUN = stringPreferencesKey("user_pronoun")
        val KEY_AI_NAME = stringPreferencesKey("ai_name")
        val KEY_AI_PRONOUN = stringPreferencesKey("ai_pronoun")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                enableHaptic = preferences[KEY_ENABLE_HAPTIC] ?: true,
                enableVoice = preferences[KEY_ENABLE_VOICE] ?: true,
                enableVisual = preferences[KEY_ENABLE_VISUAL] ?: true,
                thresholdDb = preferences[KEY_THRESHOLD_DB] ?: 58.0,
                isBackgroundMonitoringEnabled = preferences[KEY_BACKGROUND_MONITORING] ?: true,
                voicePitch = preferences[KEY_VOICE_PITCH] ?: 1.0f,
                voiceSpeechRate = preferences[KEY_VOICE_SPEECH_RATE] ?: 1.0f,
                voicePersona = preferences[KEY_VOICE_PERSONA] ?: "DEFAULT",
                userName = preferences[KEY_USER_NAME] ?: "Bạn",
                userPronoun = preferences[KEY_USER_PRONOUN] ?: "bạn",
                aiName = preferences[KEY_AI_NAME] ?: "SenseBridge",
                aiPronoun = preferences[KEY_AI_PRONOUN] ?: "tôi",
                isOnboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false
            )
        }

    val userProfileFlow: Flow<UserProfile> = userPreferencesFlow.map { it.toUserProfile() }

    suspend fun updateHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLE_HAPTIC] = enabled
        }
    }

    suspend fun updateVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLE_VOICE] = enabled
        }
    }

    suspend fun updateVisualEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLE_VISUAL] = enabled
        }
    }

    suspend fun updateThresholdDb(thresholdDb: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THRESHOLD_DB] = thresholdDb
        }
    }

    suspend fun updateBackgroundMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_MONITORING] = enabled
        }
    }

    suspend fun updateVoicePitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOICE_PITCH] = pitch
        }
    }

    suspend fun updateVoiceSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOICE_SPEECH_RATE] = rate
        }
    }

    suspend fun updateVoicePersona(personaId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOICE_PERSONA] = personaId
        }
    }

    suspend fun updateUserProfile(
        userName: String,
        userPronoun: String,
        aiName: String,
        aiPronoun: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = userName.trim()
            preferences[KEY_USER_PRONOUN] = userPronoun.trim()
            preferences[KEY_AI_NAME] = aiName.trim()
            preferences[KEY_AI_PRONOUN] = aiPronoun.trim()
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}
