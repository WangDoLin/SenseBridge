package com.sensebridge.input.sound

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DecibelEnergyGateTest {

    private lateinit var energyGate: DecibelEnergyGate

    @Before
    fun setUp() {
        energyGate = DecibelEnergyGate(thresholdDb = 62.0)
    }

    @Test
    fun calculateDecibels_silentBuffer_returnsLowValue() {
        val silentBuffer = ShortArray(1024) { 0 }
        val db = energyGate.calculateDecibels(silentBuffer, silentBuffer.size)

        // Silent buffer should not trigger inference
        assertFalse(energyGate.shouldTriggerInference(silentBuffer, silentBuffer.size))
    }

    @Test
    fun calculateDecibels_loudBuffer_triggersInference() {
        // High amplitude PCM samples (e.g. loud horn or siren at amplitude ~18000)
        val loudBuffer = ShortArray(1024) { 18000.toShort() }
        val db = energyGate.calculateDecibels(loudBuffer, loudBuffer.size)

        assertTrue(db > 62.0)
        assertTrue(energyGate.shouldTriggerInference(loudBuffer, loudBuffer.size))
    }

    @Test
    fun sensitivitySwitching_changesTriggerBehavior() {
        // Moderate volume buffer (amplitude ~1500)
        val moderateBuffer = ShortArray(1024) { 1500.toShort() }

        // Normal street threshold (68 dB) -> Suppressed
        energyGate.thresholdDb = 68.0
        val triggersStreet = energyGate.shouldTriggerInference(moderateBuffer, moderateBuffer.size)

        // Quiet room threshold (55 dB) -> Triggers
        energyGate.thresholdDb = 55.0
        val triggersQuietRoom = energyGate.shouldTriggerInference(moderateBuffer, moderateBuffer.size)

        assertFalse(triggersStreet)
        assertTrue(triggersQuietRoom)
    }
}
