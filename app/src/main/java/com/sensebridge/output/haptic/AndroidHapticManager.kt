package com.sensebridge.output.haptic

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
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
        private const val NO_REPEAT = -1
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
        val effect = createVibrationEffect(priority) ?: return
        play(effect, priority)
    }

    override fun triggerForLabel(label: String, priority: PriorityLevel) {
        val timings = HapticSignatures.timingsFor(label) ?: return trigger(priority)
        play(createSignatureEffect(timings, priority), priority)
    }

    private fun createSignatureEffect(timings: LongArray, priority: PriorityLevel): VibrationEffect {
        if (!hasAmplitudeControl) return VibrationEffect.createWaveform(timings, NO_REPEAT)
        val amplitude = if (priority.isUrgent) HapticSignatures.URGENT_AMPLITUDE else HapticSignatures.NORMAL_AMPLITUDE
        return VibrationEffect.createWaveform(timings, HapticSignatures.amplitudesFor(timings, amplitude), NO_REPEAT)
    }

    private fun play(effect: VibrationEffect, priority: PriorityLevel) {
        val activeVibrator = vibrator
        if (activeVibrator == null || !activeVibrator.hasVibrator()) {
            Log.w(TAG, "No vibration hardware detected on this device.")
            return
        }
        try {
            if (priority.isUrgent) vibrateAsAlarm(activeVibrator, effect) else activeVibrator.vibrate(effect)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing VIBRATE permission for priority: $priority", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid vibration waveform for priority: $priority", e)
        }
    }

    /**
     * Default-usage vibrations can be silently dropped by Android 12+ while the app is in the
     * background, the screen is off, or the ringer is silent. Danger alerts use ALARM usage.
     */
    private fun vibrateAsAlarm(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, audioAttributes)
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
