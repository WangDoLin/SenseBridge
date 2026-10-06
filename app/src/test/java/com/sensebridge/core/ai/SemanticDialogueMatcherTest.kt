package com.sensebridge.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SemanticDialogueMatcherTest {

    private lateinit var matcher: SemanticDialogueMatcher

    @Before
    fun setUp() {
        matcher = SemanticDialogueMatcher()
    }

    @Test
    fun matchSubIntent_whenAskingIdentityWithVariations_matchesIdentityInquiry() {
        val inputs = listOf(
            "Bạn tên là gì?",
            "Bạn là ai?",
            "Em tên là gì",
            "Cháu tên gì",
            "Mày là ai thế",
            "Ai đang nói đấy"
        )

        for (input in inputs) {
            val result = matcher.matchSubIntent(input)
            assertEquals("Failed for input: $input", DialogueAct.IDENTITY_INQUIRY, result.act)
            assertTrue("Confidence should be >= 0.25 for $input (got ${result.confidence})", result.confidence >= 0.25f)
        }
    }

    @Test
    fun matchSubIntent_whenGratitudeWithVariations_matchesGratitude() {
        val inputs = listOf(
            "Cảm ơn bạn nhiều",
            "Thanks nha",
            "Thank you",
            "Rất biết ơn em",
            "Đội ơn cháu nhé",
            "Tuyệt vời, cảm ơn"
        )

        for (input in inputs) {
            val result = matcher.matchSubIntent(input)
            assertEquals("Failed for input: $input", DialogueAct.GRATITUDE, result.act)
            assertTrue("Confidence should be >= 0.25 for $input (got ${result.confidence})", result.confidence >= 0.25f)
        }
    }

    @Test
    fun matchSubIntent_whenFarewell_matchesFarewell() {
        val inputs = listOf(
            "Tạm biệt nhé",
            "Bye bye",
            "Hẹn gặp lại sau",
            "Tôi đi đây",
            "Nghỉ ngơi nhé"
        )

        for (input in inputs) {
            val result = matcher.matchSubIntent(input)
            assertEquals("Failed for input: $input", DialogueAct.FAREWELL, result.act)
            assertTrue("Confidence should be >= 0.25 for $input (got ${result.confidence})", result.confidence >= 0.25f)
        }
    }

    @Test
    fun matchSubIntent_whenUnrelatedSafetyQuery_doesNotFalsePositiveSmalltalk() {
        val input = "Có qua đường an toàn không bạn?"
        val result = matcher.matchSubIntent(input, minThreshold = 0.30f)
        // Should not falsely classify safety queries as smalltalk
        assertTrue(result.confidence < 0.30f || result.act == DialogueAct.GENERAL_CHAT)
    }
}
