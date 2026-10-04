package com.sensebridge.input.sound

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
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
 * Captures live microphone audio (16 kHz mono) and turns it into sound events.
 *
 * Pipeline (see accuracy_review.md):
 * 1. 100 ms blocks → level meter + onset-based loud-impact detector (P1, instant vibration).
 * 2. Every 0.5 s (hop) → YAMNet on the latest 0.975 s window, multi-label, exact-label whitelist.
 * 3. [TemporalVoter] 2-of-3 windows (or one very confident window) confirms a sound group.
 * 4. Each confirmed window is submitted; EventEngine's episode model turns repeats into
 *    vibration-only continuations, so a horn ALWAYS vibrates without TTS/notification spam.
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
        private const val BYTES_PER_SAMPLE = 2
        private const val YAMNET_WINDOW_SIZE = 15600 // 0.975 s @ 16 kHz
        private const val INFERENCE_HOP_SAMPLES = 8000 // 0.5 s → ~50 % window overlap
        private const val READ_CHUNK_SAMPLES = 1600 // 100 ms level-meter resolution
        private const val PCM_16_FULL_SCALE = 32768.0f
        private const val WAKELOCK_TIMEOUT_MS = 24 * 60 * 60 * 1000L
        private const val LOUD_IMPACT_LABEL = "loud_sound"
        private const val LOUD_IMPACT_CONFIDENCE = 0.9f
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isBackgroundEnabled = MutableStateFlow(true)
    val isBackgroundEnabled: StateFlow<Boolean> = _isBackgroundEnabled.asStateFlow()

    private val _currentDecibels = MutableStateFlow(0.0)
    val currentDecibels: StateFlow<Double> = _currentDecibels.asStateFlow()

    private val rollingWindow = FloatArray(YAMNET_WINDOW_SIZE)
    private val temporalVoter = TemporalVoter()
    private var samplesSinceInference = 0
    private var currentHopPeakDb = Double.NEGATIVE_INFINITY
    private var previousHopPeakDb = Double.NEGATIVE_INFINITY

    private var wasPausedForStt = false

    /**
     * Temporarily pauses audio recording so SpeechRecognizer can gain exclusive access to the mic.
     */
    fun pauseForSpeechRecognition() {
        if (_isRecording.value) {
            wasPausedForStt = true
            stopListening()
            Log.i(TAG, "Temporarily paused audio recording for STT mic capture.")
        }
    }

    /**
     * Resumes audio recording if it was previously paused for speech recognition.
     */
    fun resumeAfterSpeechRecognition() {
        if (wasPausedForStt) {
            wasPausedForStt = false
            startListening()
            Log.i(TAG, "Resumed audio recording after STT finished.")
        }
    }

    /**
     * Enables or disables monitoring while the app is in the background.
     */
    fun setBackgroundMonitoring(enabled: Boolean) {
        _isBackgroundEnabled.value = enabled
    }

    /**
     * Starts microphone capture and the classification loop (no-op if already running).
     */
    @SuppressLint("MissingPermission")
    fun startListening() {
        if (_isRecording.value) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start recording: RECORD_AUDIO permission not granted.")
            return
        }
        try {
            acquireWakeLock()
            val record = createAudioRecord() ?: return stopListening()
            audioRecord = record
            resetAnalysisState()
            record.startRecording()
            _isRecording.value = true
            recordingJob = scope.launch { processAudioStream() }
            Log.i(TAG, "Audio listening started at $SAMPLE_RATE Hz (YAMNet loaded: ${soundClassifier.isModelLoaded}).")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}", e)
            stopListening()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unsupported AudioRecord configuration: ${e.message}", e)
            stopListening()
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone access denied: ${e.message}", e)
            stopListening()
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        // Keep CPU alive for continuous background awareness
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SenseBridge::AcousticWakeLock")
        wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        val minBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        // Buffer must hold at least one hop so samples aren't dropped while YAMNet runs
        val bufferBytes = maxOf(minBufferBytes * 2, INFERENCE_HOP_SAMPLES * BYTES_PER_SAMPLE)
        val record = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferBytes)
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize.")
            record.release()
            return null
        }
        return record
    }

    private fun resetAnalysisState() {
        rollingWindow.fill(0f)
        temporalVoter.reset()
        samplesSinceInference = 0
        currentHopPeakDb = Double.NEGATIVE_INFINITY
        previousHopPeakDb = Double.NEGATIVE_INFINITY
    }

    private fun processAudioStream() {
        val chunk = ShortArray(READ_CHUNK_SAMPLES)
        val record = audioRecord ?: return
        while (scope.isActive && _isRecording.value) {
            val readSize = try {
                record.read(chunk, 0, chunk.size)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from AudioRecord: ${e.message}", e)
                break
            }
            if (readSize > 0 && _isRecording.value) {
                handleChunk(chunk, readSize)
            } else if (readSize < 0) {
                Log.w(TAG, "AudioRecord read returned error code: $readSize")
                break
            }
        }
    }

    private fun handleChunk(chunk: ShortArray, readSize: Int) {
        val decibels = energyGate.calculateDecibels(chunk, readSize)
        _currentDecibels.value = decibels
        pushToRollingWindow(chunk, readSize)

        if (energyGate.isLoudImpactOnset(decibels)) submitLoudImpact(decibels)

        currentHopPeakDb = maxOf(currentHopPeakDb, decibels)
        samplesSinceInference += readSize
        if (samplesSinceInference >= INFERENCE_HOP_SAMPLES) {
            runInferenceStep()
            samplesSinceInference = 0
            previousHopPeakDb = currentHopPeakDb
            currentHopPeakDb = Double.NEGATIVE_INFINITY
        }
    }

    /**
     * The 0.975 s window spans the last two hops, so the gate uses the peak of both; otherwise a
     * short honk would only ever get one vote from the temporal voter.
     */
    private fun runInferenceStep() {
        val windowPeakDb = maxOf(currentHopPeakDb, previousHopPeakDb)
        val groupScores = if (energyGate.shouldRunInference(windowPeakDb)) {
            soundClassifier.classifyGroups(rollingWindow)
        } else {
            emptyMap()
        }
        val confirmed = temporalVoter.submit(groupScores)
        confirmed.forEach { (group, score) -> submitGroupEvent(group, score, windowPeakDb) }
    }

    private fun submitGroupEvent(group: SoundGroup, score: Float, levelDb: Double) {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AUDIO_CLASSIFIER,
                label = group.key,
                displayTitle = "${group.vietnameseTitle} (${levelDb.toInt()} dB)",
                spokenText = group.spokenText,
                confidence = score,
                priority = group.priority
            )
        )
    }

    private fun submitLoudImpact(decibels: Double) {
        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.AUDIO_CLASSIFIER,
                label = LOUD_IMPACT_LABEL,
                displayTitle = "⚠️ Tiếng động rất lớn (${decibels.toInt()} dB)",
                spokenText = "Cảnh báo, có tiếng động lớn bất thường xung quanh bạn!",
                confidence = LOUD_IMPACT_CONFIDENCE,
                priority = PriorityLevel.WARNING_P1
            )
        )
    }

    private fun pushToRollingWindow(shorts: ShortArray, size: Int) {
        val copySize = minOf(size, YAMNET_WINDOW_SIZE)
        val keep = YAMNET_WINDOW_SIZE - copySize
        System.arraycopy(rollingWindow, copySize, rollingWindow, 0, keep)
        for (i in 0 until copySize) {
            rollingWindow[keep + i] = shorts[i] / PCM_16_FULL_SCALE
        }
    }

    /**
     * Stops capture, releases the microphone and the wake lock.
     */
    fun stopListening() {
        if (!_isRecording.value && audioRecord == null) return
        _isRecording.value = false

        val job = recordingJob
        recordingJob = null
        val record = audioRecord
        audioRecord = null

        try {
            record?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}", e)
        }
        job?.cancel()

        try {
            record?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}", e)
        } finally {
            _currentDecibels.value = 0.0
            if (wakeLock?.isHeld == true) {
                try {
                    wakeLock?.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing WakeLock: ${e.message}", e)
                }
            }
            wakeLock = null
            Log.i(TAG, "Audio listening stopped.")
        }
    }
}
