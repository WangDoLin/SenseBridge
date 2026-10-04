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
    fun calculateDecibels_silentBuffer_doesNotTrigger() {
        val silentBuffer = ShortArray(BUFFER_SIZE) { 0 }
        assertTrue(energyGate.calculateDecibels(silentBuffer, silentBuffer.size) < 0.0)
        assertFalse(energyGate.shouldTriggerInference(silentBuffer, silentBuffer.size))
    }

    @Test
    fun calculateDecibels_loudBuffer_triggersInference() {
        val loudBuffer = ShortArray(BUFFER_SIZE) { LOUD_AMPLITUDE }
        assertTrue(energyGate.calculateDecibels(loudBuffer, loudBuffer.size) > 62.0)
        assertTrue(energyGate.shouldTriggerInference(loudBuffer, loudBuffer.size))
    }

    @Test
    fun sensitivitySwitching_changesTriggerBehavior() {
        val moderateBuffer = ShortArray(BUFFER_SIZE) { MODERATE_AMPLITUDE } // ≈ 63.5 dB

        energyGate.thresholdDb = 68.0
        val triggersStreet = energyGate.shouldTriggerInference(moderateBuffer, moderateBuffer.size)

        energyGate.thresholdDb = 55.0
        val triggersQuietRoom = energyGate.shouldTriggerInference(moderateBuffer, moderateBuffer.size)

        assertFalse(triggersStreet)
        assertTrue(triggersQuietRoom)
    }

    @Test
    fun steadyLoudTone_isStillSignificant() {
        // Regression: the old variance filter treated steady tones (horn, fire alarm) as fan noise
        val steadyTone = ShortArray(BUFFER_SIZE) { if (it % 2 == 0) LOUD_AMPLITUDE else (-LOUD_AMPLITUDE).toShort() }
        assertTrue(energyGate.shouldTriggerInference(steadyTone, steadyTone.size))
    }

    @Test
    fun shouldRunInference_quietDoorbellAboveFloor_runs() {
        energyGate.thresholdDb = 68.0
        // Noise floor starts at 42 dB; 50 dB is below threshold but clearly above the floor
        assertTrue(energyGate.shouldRunInference(50.0))
        assertFalse(energyGate.shouldRunInference(44.0))
    }

    @Test
    fun loudImpactOnset_suddenBang_detected() {
        repeat(QUIET_BLOCKS) { energyGate.isLoudImpactOnset(QUIET_DB) }
        assertTrue(energyGate.isLoudImpactOnset(BANG_DB))
    }

    @Test
    fun loudImpactOnset_continuouslyLoudEnvironment_notRepeated() {
        repeat(LOUD_BLOCKS) { energyGate.isLoudImpactOnset(BANG_DB) }
        assertFalse(energyGate.isLoudImpactOnset(BANG_DB))
    }

    private companion object {
        const val BUFFER_SIZE = 1024
        const val LOUD_AMPLITUDE: Short = 18000
        const val MODERATE_AMPLITUDE: Short = 1500
        const val QUIET_DB = 50.0
        const val BANG_DB = 88.0
        const val QUIET_BLOCKS = 10
        const val LOUD_BLOCKS = 20
    }
}
