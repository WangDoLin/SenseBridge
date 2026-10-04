package com.sensebridge.core.monitoring

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.sensebridge.core.service.SenseBridgeForegroundService
import com.sensebridge.data.repository.UserPreferencesRepository
import com.sensebridge.input.sound.AudioRecorderManager
import com.sensebridge.input.sound.DecibelEnergyGate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified coordinator and single source of truth for audio monitoring and background service lifecycle.
 *
 * Eliminates race conditions and divergent states across Home, DeafAssist, Settings, and MainActivity.
 */
@Singleton
class MonitoringCoordinator(
    private val context: Context,
    private val audioRecorderManager: AudioRecorderManager,
    private val energyGate: DecibelEnergyGate,
    private val preferencesRepository: UserPreferencesRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        audioRecorderManager: AudioRecorderManager,
        energyGate: DecibelEnergyGate,
        preferencesRepository: UserPreferencesRepository
    ) : this(context, audioRecorderManager, energyGate, preferencesRepository, Dispatchers.Default)

    companion object {
        private const val TAG = "MonitoringCoordinator"
    }

    private val coordinatorScope = CoroutineScope(dispatcher + SupervisorJob())

    val isRecording: StateFlow<Boolean> = audioRecorderManager.isRecording
    val currentDecibels: StateFlow<Double> = audioRecorderManager.currentDecibels

    private val _isBackgroundEnabled = MutableStateFlow(true)
    val isBackgroundEnabled: StateFlow<Boolean> = _isBackgroundEnabled.asStateFlow()

    init {
        coordinatorScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _isBackgroundEnabled.value = prefs.isBackgroundMonitoringEnabled
                audioRecorderManager.setBackgroundMonitoring(prefs.isBackgroundMonitoringEnabled)
                energyGate.thresholdDb = prefs.thresholdDb
                Log.d(TAG, "Preferences synced: bgEnabled=${prefs.isBackgroundMonitoringEnabled}, thresholdDb=${prefs.thresholdDb}")
            }
        }
    }

    /**
     * Starts listening. If background monitoring is enabled, also launches the foreground service.
     */
    fun startMonitoring() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start monitoring: RECORD_AUDIO permission not granted.")
            return
        }

        if (_isBackgroundEnabled.value) {
            SenseBridgeForegroundService.start(context)
        }
        audioRecorderManager.startListening()
    }

    /**
     * Stops listening and terminates the foreground service.
     */
    fun stopMonitoring() {
        audioRecorderManager.stopListening()
        SenseBridgeForegroundService.stop(context)
    }

    /**
     * Toggles the current monitoring state.
     */
    fun toggleMonitoring() {
        if (isRecording.value) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    /**
     * Updates whether background monitoring is desired and persists the preference.
     */
    fun setBackgroundMonitoring(enabled: Boolean) {
        _isBackgroundEnabled.value = enabled
        audioRecorderManager.setBackgroundMonitoring(enabled)

        coordinatorScope.launch {
            preferencesRepository.updateBackgroundMonitoringEnabled(enabled)
        }

        // If actively monitoring, reconcile foreground service state immediately
        if (isRecording.value) {
            if (enabled) {
                SenseBridgeForegroundService.start(context)
            } else {
                SenseBridgeForegroundService.stop(context)
            }
        }
    }

    /**
     * Invoked on app startup or when permission is granted.
     * Starts background monitoring ONLY IF user has previously enabled it.
     */
    fun startIfPermittedAndEnabled() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && _isBackgroundEnabled.value) {
            Log.i(TAG, "Auto-starting monitoring based on user preference.")
            startMonitoring()
        } else {
            Log.d(TAG, "Auto-start skipped: hasPermission=$hasPermission, bgEnabled=${_isBackgroundEnabled.value}")
        }
    }
}
