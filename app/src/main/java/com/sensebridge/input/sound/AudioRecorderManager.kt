package com.sensebridge.input.sound

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures live microphone audio at 16kHz Mono and feeds energetic segments to SoundClassifier.
 */
@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val energyGate: DecibelEnergyGate,
    private val soundClassifier: SoundClassifier,
    private val eventEngine: EventEngine
) {
    companion object {
        private const val TAG = "AudioRecorderManager"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentDecibels = MutableStateFlow(0.0)
    val currentDecibels: StateFlow<Double> = _currentDecibels.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (_isRecording.value) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start recording: RECORD_AUDIO permission not granted.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize.")
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch {
                processAudioStream(bufferSize)
            }
            Log.i(TAG, "Audio listening started at $SAMPLE_RATE Hz.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}", e)
            stopListening()
        }
    }

    private suspend fun processAudioStream(bufferSize: Int) {
        val shortBuffer = ShortArray(bufferSize)

        while (scope.isActive && _isRecording.value) {
            val record = audioRecord ?: break
            val readSize = record.read(shortBuffer, 0, bufferSize)

            if (readSize > 0) {
                val decibels = energyGate.calculateDecibels(shortBuffer, readSize)
                _currentDecibels.value = decibels

                // Gate: Only invoke AI when sound exceeds background noise threshold
                if (decibels >= energyGate.thresholdDb) {
                    val floatBuffer = convertShortsToNormalizedFloats(shortBuffer, readSize)
                    val result = soundClassifier.classify(floatBuffer)

                    if (result != null) {
                        eventEngine.submitEvent(
                            SenseEvent(
                                source = SensorySource.AUDIO_CLASSIFIER,
                                label = result.label,
                                displayTitle = result.vietnameseTitle,
                                spokenText = result.spokenText,
                                confidence = result.confidence,
                                priority = result.priority
                            )
                        )
                    }
                }
            }
        }
    }

    private fun convertShortsToNormalizedFloats(shorts: ShortArray, size: Int): FloatArray {
        val floats = FloatArray(size)
        for (i in 0 until size) {
            floats[i] = shorts[i] / 32768.0f
        }
        return floats
    }

    fun stopListening() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
            _currentDecibels.value = 0.0
            Log.i(TAG, "Audio listening stopped.")
        }
    }
}
