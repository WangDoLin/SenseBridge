package com.sensebridge.input.sound

import com.sensebridge.core.model.PriorityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uses REAL YAMNet label names (from yamnet_label_list.txt embedded in assets/yamnet.tflite).
 */
class AudioLabelTaxonomyTest {

    @Test
    fun mapLabel_carHorn_mapsToCriticalP0() {
        val result = AudioLabelTaxonomy.mapLabel("Vehicle horn, car horn, honking", 0.88f)
        assertEquals("car_horn", result?.label)
        assertEquals(PriorityLevel.CRITICAL_P0, result?.priority)
        assertTrue(result?.spokenText?.contains("còi xe") == true)
    }

    @Test
    fun mapLabel_smokeDetector_mapsToFireAlarm() {
        val result = AudioLabelTaxonomy.mapLabel("Smoke detector, smoke alarm", 0.94f)
        assertEquals("fire_alarm", result?.label)
        assertEquals(PriorityLevel.CRITICAL_P0, result?.priority)
    }

    @Test
    fun mapLabel_doorbellAndBabyCry_mapCorrectPriorities() {
        assertEquals(PriorityLevel.ATTENTION_P2, AudioLabelTaxonomy.mapLabel("Doorbell", 0.8f)?.priority)
        assertEquals(PriorityLevel.WARNING_P1, AudioLabelTaxonomy.mapLabel("Baby cry, infant cry", 0.8f)?.priority)
    }

    @Test
    fun mapLabel_confusingLookalikes_returnNull() {
        // Substring matching used to map these to car horn / knock / doorbell / glass break
        listOf("French horn", "Engine knocking", "Wind chime", "Glass", "Train horn", "Foghorn")
            .forEach { assertNull("$it must not raise an alert", AudioLabelTaxonomy.mapLabel(it, 0.95f)) }
    }

    @Test
    fun mapLabel_everydayNoise_returnsNull() {
        listOf("Mechanical fan", "Speech", "Vehicle", "Music", "Air conditioning")
            .forEach { assertNull(AudioLabelTaxonomy.mapLabel(it, 0.99f)) }
    }

    @Test
    fun scoreGroups_hornBelowVehicleTop1_isStillDetected() {
        val scores = AudioLabelTaxonomy.scoreGroups(
            listOf("Vehicle" to 0.80f, "Car" to 0.55f, "Vehicle horn, car horn, honking" to 0.42f)
        )
        assertEquals(0.42f, scores[SoundGroup.CAR_HORN] ?: 0f, FLOAT_TOLERANCE)
        assertTrue(SoundGroup.CAR_HORN in AudioLabelTaxonomy.passingGroups(scores))
    }

    @Test
    fun scoreGroups_takesMaxAcrossMemberLabels() {
        val scores = AudioLabelTaxonomy.scoreGroups(listOf("Siren" to 0.3f, "Ambulance (siren)" to 0.6f))
        assertEquals(0.6f, scores[SoundGroup.SIREN] ?: 0f, FLOAT_TOLERANCE)
    }

    @Test
    fun mostUrgent_prefersP0OverHigherScoringP2() {
        val groups = mapOf(SoundGroup.DOORBELL to 0.9f, SoundGroup.SIREN to 0.4f)
        assertEquals(SoundGroup.SIREN, AudioLabelTaxonomy.mostUrgent(groups))
    }

    @Test
    fun soundGroupKeys_areUnique() {
        val keys = SoundGroup.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun yamnetLabels_belongToExactlyOneGroup() {
        val labels = SoundGroup.entries.flatMap { it.yamnetLabels }
        assertEquals(labels.size, labels.toSet().size)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 1e-6f
    }
}
