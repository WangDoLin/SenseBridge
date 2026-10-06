package com.sensebridge.output.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePersonaTest {

    @Test
    fun voicePersona_allEntries_haveValidPitchAndRateRanges() {
        VoicePersona.entries.forEach { persona ->
            assertTrue("Pitch must be positive for ${persona.name}", persona.pitch in 0.5f..2.0f)
            assertTrue("SpeechRate must be positive for ${persona.name}", persona.speechRate in 0.5f..2.5f)
            assertTrue("DisplayName must not be empty", persona.displayNameVi.isNotBlank())
            assertTrue("Description must not be empty", persona.descriptionVi.isNotBlank())
        }
    }

    @Test
    fun voicePersona_fromId_resolvesAccurately() {
        assertEquals(VoicePersona.DEFAULT, VoicePersona.fromId("DEFAULT"))
        assertEquals(VoicePersona.WARM_MALE, VoicePersona.fromId("WARM_MALE"))
        assertEquals(VoicePersona.WARM_MALE, VoicePersona.fromId("warm_male"))
        assertEquals(VoicePersona.NATURAL_FEMALE, VoicePersona.fromId("NATURAL_FEMALE"))
        assertEquals(VoicePersona.ENERGETIC_ASSISTANT, VoicePersona.fromId("ENERGETIC_ASSISTANT"))
        assertEquals(VoicePersona.CALM_NARRATOR, VoicePersona.fromId("CALM_NARRATOR"))
        // Fallback to DEFAULT on unknown id
        assertEquals(VoicePersona.DEFAULT, VoicePersona.fromId("NON_EXISTENT_ID"))
    }

    @Test
    fun samplePhrase_isNonEmptyVietnameseText() {
        assertNotNull(VoicePersona.SAMPLE_PHRASE)
        assertTrue(VoicePersona.SAMPLE_PHRASE.contains("SenseBridge"))
    }
}
