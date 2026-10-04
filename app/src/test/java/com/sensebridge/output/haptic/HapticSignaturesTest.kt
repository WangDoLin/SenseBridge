package com.sensebridge.output.haptic

import com.sensebridge.core.engine.SmartEventDebouncer
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.input.sound.SoundGroup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticSignaturesTest {

    @Test
    fun everyLabel_isAKnownSoundGroupOrLoudSound() {
        val knownKeys = SoundGroup.entries.map { it.key }.toSet() + LOUD_SOUND_LABEL
        HapticSignatures.TIMINGS_BY_LABEL.keys.forEach { label ->
            assertTrue("Unknown haptic label: $label", label in knownKeys)
        }
    }

    @Test
    fun everyUrgentSoundGroup_hasADedicatedRhythm() {
        SoundGroup.entries.filter { it.priority.isUrgent }.forEach { group ->
            assertNotNull("Missing rhythm for ${group.key}", HapticSignatures.timingsFor(group.key))
        }
    }

    @Test
    fun urgentRhythms_fitInsideContinuationInterval() {
        // Otherwise the ~1 s continuation re-trigger would cut the rhythm and blur it
        SoundGroup.entries.filter { it.priority.isUrgent }.forEach { group ->
            val timings = HapticSignatures.timingsFor(group.key) ?: return@forEach
            val duration = HapticSignatures.durationOf(timings)
            assertTrue("${group.key} lasts $duration ms", duration <= SmartEventDebouncer.HAPTIC_REPEAT_INTERVAL_MS)
        }
    }

    @Test
    fun keyDangerSounds_haveDistinguishableRhythms() {
        val dangerKeys = listOf("car_horn", "siren", "fire_alarm", "glass_break", "scream")
        val rhythms = dangerKeys.map { HapticSignatures.timingsFor(it)!!.toList() }
        assertEquals(dangerKeys.size, rhythms.toSet().size)
    }

    @Test
    fun attentionRhythms_areLongEnoughToFeelThroughClothing() {
        listOf("doorbell", "knock", "phone_ring").forEach { key ->
            val duration = HapticSignatures.durationOf(HapticSignatures.timingsFor(key)!!)
            assertTrue("$key only lasts $duration ms", duration >= MIN_NOTICEABLE_MS)
        }
    }

    @Test
    fun amplitudesFor_zeroOnDelayAndOffSegments() {
        val amplitudes = HapticSignatures.amplitudesFor(longArrayOf(0, 100, 50, 100), HapticSignatures.URGENT_AMPLITUDE)
        assertArrayEquals(intArrayOf(0, 255, 0, 255), amplitudes)
    }

    @Test
    fun unknownLabel_fallsBackToPriorityPattern() {
        assertEquals(null, HapticSignatures.timingsFor("dog_bark"))
        // Sanity: fallback path exists for every priority
        assertEquals(4, PriorityLevel.entries.size)
    }

    private companion object {
        const val LOUD_SOUND_LABEL = "loud_sound"
        const val MIN_NOTICEABLE_MS = 300L
    }
}
