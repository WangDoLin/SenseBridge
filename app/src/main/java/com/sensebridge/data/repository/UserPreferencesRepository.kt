package com.sensebridge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
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
    val isBackgroundMonitoringEnabled: Boolean = true
)

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
                isBackgroundMonitoringEnabled = preferences[KEY_BACKGROUND_MONITORING] ?: true
            )
        }

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
}
