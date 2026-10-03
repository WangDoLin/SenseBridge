package com.sensebridge.input.sound

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Energy gating module to avoid running heavy neural network inferences
 * during silent or ambient background conditions, preserving battery.
 *
 * @param thresholdDb Minimum decibel level required to trigger AI inference (default: 62.0 dB).
 */
class DecibelEnergyGate(
    var thresholdDb: Double = DEFAULT_THRESHOLD_DB
) {
    companion object {
        const val DEFAULT_THRESHOLD_DB = 62.0
        private const val REFERENCE_AMPLITUDE = 1.0
        private const val MIN_AMPLITUDE = 1e-6
    }

    /**
     * Calculates the decibel level of a PCM 16-bit mono buffer.
     *
     * @param buffer Raw audio samples.
     * @param readSize Number of valid samples in the buffer.
     * @return Decibel value (typically in the range 20 dB to 95+ dB).
     */
    fun calculateDecibels(buffer: ShortArray, readSize: Int): Double {
        if (readSize <= 0) return 0.0

        var sumSquare = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquare += sample * sample
        }

        val rms = sqrt(sumSquare / readSize)
        val normalizedRms = if (rms < MIN_AMPLITUDE) MIN_AMPLITUDE else rms

        // Standard acoustic formula: 20 * log10(RMS / Reference)
        return 20.0 * log10(normalizedRms / REFERENCE_AMPLITUDE)
    }

    /**
     * Evaluates if the buffer contains sufficient acoustic energy to warrant AI classification.
     *
     * @param buffer Raw audio samples.
     * @param readSize Number of valid samples in the buffer.
     * @return True if energy exceeds the activation threshold; false to skip inference.
     */
    fun shouldTriggerInference(buffer: ShortArray, readSize: Int): Boolean {
        val currentDb = calculateDecibels(buffer, readSize)
        return currentDb >= thresholdDb
    }
}
