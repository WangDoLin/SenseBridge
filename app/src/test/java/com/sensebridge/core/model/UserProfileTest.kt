package com.sensebridge.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun userProfile_defaultValues_areSoundAndSafe() {
        val defaultProfile = UserProfile.DEFAULT
        assertEquals("bạn", defaultProfile.userName)
        assertEquals("bạn", defaultProfile.userPronoun)
        assertEquals("SenseBridge", defaultProfile.aiName)
        assertEquals("mình", defaultProfile.aiPronoun)
        assertFalse(defaultProfile.isOnboardingCompleted)
        assertTrue(defaultProfile.buildGreeting().contains("Xin chào bạn! Mình là AI SenseBridge"))
    }

    @Test
    fun userProfile_customizedGreeting_formatsNaturalVietnameseSpeech() {
        val profile = UserProfile(
            userName = "Lâm",
            userPronoun = "anh",
            aiName = "Mimi",
            aiPronoun = "em",
            isOnboardingCompleted = true
        )

        val greeting = profile.buildGreeting()
        assertTrue("Greeting should contain 'Xin chào anh Lâm'", greeting.contains("Xin chào anh Lâm"))
        assertTrue("Greeting should contain AI name 'Mimi'", greeting.contains("Em là Mimi"))
        assertTrue("Greeting should use pronoun 'anh'", greeting.contains("hỗ trợ anh"))

        val identity = profile.buildIdentityReply()
        assertTrue("Identity should state 'Em là Mimi'", identity.contains("Em là Mimi"))
        assertTrue("Identity should reference 'anh Lâm'", identity.contains("anh Lâm"))

        val thanks = profile.buildGratitudeReply()
        assertTrue("Thanks should address 'anh Lâm'", thanks.contains("anh Lâm"))
        assertTrue("Thanks should reference 'Mimi'", thanks.contains("bảo Mimi"))
    }

    @Test
    fun userProfile_formatText_replacesAllPlaceholdersAccurately() {
        val profile = UserProfile(
            userName = "Hải",
            userPronoun = "bác",
            aiName = "Lumi",
            aiPronoun = "cháu",
            isOnboardingCompleted = true
        )

        val template = "{CAP_AI_PRONOUN} luôn ở bên {USER}. {CAP_USER_PRONOUN} cứ yên tâm vào {AI_NAME} nhé."
        val formatted = profile.formatText(template)

        assertEquals("Cháu luôn ở bên bác Hải. Bác cứ yên tâm vào Lumi nhé.", formatted)
    }
}
