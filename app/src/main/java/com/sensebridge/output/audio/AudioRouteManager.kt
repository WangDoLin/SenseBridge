package com.sensebridge.output.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Bluetooth Audio routing and monitors connection state.
 * Implements privacy protection: automatically mutes speech output when headset disconnects.
 */
@Singleton
class AudioRouteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioRouteManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isMutedForPrivacy = MutableStateFlow(false)
    val isMutedForPrivacy: StateFlow<Boolean> = _isMutedForPrivacy.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            evaluateAudioDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val hadBluetooth = _isBluetoothConnected.value
            evaluateAudioDevices()
            // If Bluetooth was lost, engage privacy mute
            if (hadBluetooth && !_isBluetoothConnected.value) {
                Log.w(TAG, "Bluetooth headset disconnected! Activating privacy mute.")
                _isMutedForPrivacy.value = true
            }
        }
    }

    init {
        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
        evaluateAudioDevices()
    }

    /**
     * Inspects attached audio devices and routes communication to Bluetooth headset if available.
     */
    fun evaluateAudioDevices() {
        val manager = audioManager ?: return
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val bluetoothDevice = devices.firstOrNull { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }

        if (bluetoothDevice != null) {
            _isBluetoothConnected.value = true
            _connectedDeviceName.value = bluetoothDevice.productName.toString()
            _isMutedForPrivacy.value = false // Auto unmute when verified connected
            routeToDevice(bluetoothDevice)
        } else {
            _isBluetoothConnected.value = false
            _connectedDeviceName.value = null
        }
    }

    /**
     * Routes audio communication to target device using modern Android 12+ API.
     */
    private fun routeToDevice(deviceInfo: AudioDeviceInfo) {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val success = manager.setCommunicationDevice(deviceInfo)
            Log.d(TAG, "setCommunicationDevice(${deviceInfo.productName}) status: $success")
        }
    }

    /**
     * Allows the user to manually override privacy mute and use phone speaker if desired.
     */
    fun uncheckPrivacyMute() {
        _isMutedForPrivacy.value = false
    }

    /**
     * Cleans up registered audio callbacks.
     */
    fun release() {
        audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager?.clearCommunicationDevice()
        }
    }
}
