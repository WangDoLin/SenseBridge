package com.sensebridge.output.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.sensebridge.core.model.PriorityLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of HapticManager leveraging VibrationEffect and system Vibrator.
 */
@Singleton
class AndroidHapticManager @Inject constructor(
    @ApplicationContext private val context: Context
) : HapticManager {

    companion object {
        private const val TAG = "AndroidHapticManager"
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override val hasAmplitudeControl: Boolean
        get() = vibrator?.hasAmplitudeControl() == true

    override fun trigger(priority: PriorityLevel) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.w(TAG, "No vibration hardware detected on this device.")
            return
        }

        try {
            val effect = createVibrationEffect(priority)
            if (effect != null) {
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute vibration for priority: $priority", e)
        }
    }

    override fun cancel() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel vibration", e)
        }
    }

    private fun createVibrationEffect(priority: PriorityLevel): VibrationEffect? {
        val canModulate = hasAmplitudeControl

        return when (priority) {
            PriorityLevel.CRITICAL_P0 -> {
                if (canModulate) {
                    VibrationEffect.createWaveform(
                        HapticPattern.P0_TIMINGS,
                        HapticPattern.P0_AMPLITUDES,
                        -1
                    )
                } else {
                    VibrationEffect.createWaveform(HapticPattern.P0_TIMINGS, -1)
                }
            }
            PriorityLevel.WARNING_P1 -> {
                if (canModulate) {
                    VibrationEffect.createWaveform(
                        HapticPattern.P1_TIMINGS,
                        HapticPattern.P1_AMPLITUDES,
                        -1
                    )
                } else {
                    VibrationEffect.createWaveform(HapticPattern.P1_TIMINGS, -1)
                }
            }
            PriorityLevel.ATTENTION_P2 -> {
                if (canModulate) {
                    VibrationEffect.createOneShot(
                        HapticPattern.P2_DURATION_MS,
                        HapticPattern.P2_AMPLITUDE
                    )
                } else {
                    VibrationEffect.createOneShot(
                        HapticPattern.P2_DURATION_MS,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                }
            }
            PriorityLevel.INFO_P3 -> {
                if (canModulate) {
                    VibrationEffect.createOneShot(
                        HapticPattern.P3_DURATION_MS,
                        HapticPattern.P3_AMPLITUDE
                    )
                } else {
                    null // Omit P3 vibration on basic motors to prevent buzzing fatigue
                }
            }
        }
    }
}
