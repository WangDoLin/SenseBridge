package com.sensebridge.core.ai

import com.sensebridge.core.model.UserProfile
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonaAwareDialogueGeneratorTest {

    private lateinit var generator: PersonaAwareDialogueGenerator

    @Before
    fun setUp() {
        generator = PersonaAwareDialogueGenerator()
    }

    @Test
    fun generateSmalltalk_defaultProfile_containsExpectedKeywords() {
        val identity = generator.generateSmalltalk(DialogueAct.IDENTITY_INQUIRY, UserProfile.DEFAULT)
        assertTrue(identity.contains("SenseAI"))

        val gratitude = generator.generateSmalltalk(DialogueAct.GRATITUDE, UserProfile.DEFAULT)
        assertTrue(gratitude.contains("Rất vui được hỗ trợ"))
    }

    @Test
    fun generateSmalltalk_elderProfile_usesRespectfulHonorifics() {
        val profile = UserProfile(
            userName = "Ba",
            userPronoun = "bác",
            aiName = "Lumi",
            aiPronoun = "cháu",
            isOnboardingCompleted = true
        )

        val gratitude = generator.generateSmalltalk(DialogueAct.GRATITUDE, profile, randomSeed = 42)
        assertTrue(gratitude.contains("bác") || gratitude.contains("cháu"))

        val farewell = generator.generateSmalltalk(DialogueAct.FAREWELL, profile, randomSeed = 42)
        assertTrue(farewell.contains("bác"))
    }
}
