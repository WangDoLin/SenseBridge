package com.sensebridge.input.sound

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Acoustic level meter and gates.
 *
 * NOTE: values are relative dBFS + ~90 (20·log10 of 16-bit RMS), NOT calibrated SPL.
 *
 * Design decisions (see accuracy_review.md):
 * - Fans/AC are rejected by the YAMNet whitelist, not by an energy-variance heuristic.
 *   The old coefficient-of-variation filter also suppressed steady horns and fire alarms.
 * - The noise floor only adapts on quiet blocks (capped) so loud events never raise it.
 * - Loud-impact detection is onset-based (jump above the recent median), so a continuously
 *   loud environment (street, concert) does not vibrate forever.
 */
class DecibelEnergyGate(
    var thresholdDb: Double = DEFAULT_THRESHOLD_DB
) {
    companion object {
        const val DEFAULT_THRESHOLD_DB = 58.0
        /** Anything this loud is always worth analysing, whatever the sensitivity. */
        const val ALWAYS_SIGNIFICANT_DB = 68.0
        /** Required margin above the noise floor for a sound to count as significant. */
        const val MIN_SNR_DB = 8.0
        /** Lower margin used only to decide whether running YAMNet is worthwhile. */
        const val INFERENCE_MIN_SNR_DB = 6.0
        const val LOUD_IMPACT_DB = 84.0
        const val IMPACT_ONSET_DELTA_DB = 15.0
        private const val INITIAL_NOISE_FLOOR_DB = 42.0
        private const val MAX_NOISE_FLOOR_CAP = 54.0
        private const val EMA_ALPHA = 0.05
        private const val FLOOR_UPDATE_INTERVAL_MS = 200L
        private const val MIN_AMPLITUDE = 1e-6
        private const val DB_PER_DECADE = 20.0
        private const val LEVEL_HISTORY_SIZE = 24
    }

    private var noiseFloorDb = INITIAL_NOISE_FLOOR_DB
    private var lastFloorUpdateTime = 0L
    private val recentLevels = ArrayDeque<Double>()

    /**
     * Calculates the level of a PCM 16-bit mono buffer and updates the adaptive noise floor.
     */
    fun calculateDecibels(buffer: ShortArray, readSize: Int): Double {
        if (readSize <= 0) return 0.0
        var sumSquare = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquare += sample * sample
        }
        val rms = sqrt(sumSquare / readSize).coerceAtLeast(MIN_AMPLITUDE)
        val currentDb = DB_PER_DECADE * log10(rms)
        updateNoiseFloor(currentDb)
        return currentDb
    }

    private fun updateNoiseFloor(currentDb: Double) {
        val now = System.currentTimeMillis()
        if (now - lastFloorUpdateTime <= FLOOR_UPDATE_INTERVAL_MS) return
        lastFloorUpdateTime = now
        if (currentDb < MAX_NOISE_FLOOR_CAP) {
            noiseFloorDb = (1.0 - EMA_ALPHA) * noiseFloorDb + EMA_ALPHA * currentDb
        }
    }

    /**
     * True when the level is loud enough (relative to sensitivity and noise floor) to matter.
     */
    fun isSignificantTransientSpike(currentDb: Double): Boolean {
        if (currentDb >= ALWAYS_SIGNIFICANT_DB) return true
        return currentDb >= thresholdDb && currentDb - noiseFloorDb >= MIN_SNR_DB
    }

    /**
     * Convenience wrapper: measures [buffer] and applies [isSignificantTransientSpike].
     */
    fun shouldTriggerInference(buffer: ShortArray, readSize: Int): Boolean =
        isSignificantTransientSpike(calculateDecibels(buffer, readSize))

    /**
     * Generous gate for YAMNet: run the model when the window peak is either above the user
     * threshold or clearly above the noise floor. False positives are handled by the whitelist,
     * so missing a quiet doorbell is the bigger risk here.
     */
    fun shouldRunInference(windowPeakDb: Double): Boolean =
        windowPeakDb >= thresholdDb || windowPeakDb - noiseFloorDb >= INFERENCE_MIN_SNR_DB

    /**
     * Detects a sudden very loud impact: [currentDb] is above [LOUD_IMPACT_DB] AND jumps at
     * least [IMPACT_ONSET_DELTA_DB] above the median of recent blocks.
     */
    fun isLoudImpactOnset(currentDb: Double): Boolean {
        val baseline = median(recentLevels)
        recentLevels.addLast(currentDb)
        while (recentLevels.size > LEVEL_HISTORY_SIZE) recentLevels.removeFirst()
        val reference = baseline ?: noiseFloorDb
        return currentDb >= LOUD_IMPACT_DB && currentDb - reference >= IMPACT_ONSET_DELTA_DB
    }

    private fun median(values: Collection<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    fun getBackgroundNoiseFloor(): Double = noiseFloorDb
}
