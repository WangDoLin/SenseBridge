package com.sensebridge.core.ai

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SenseAiNeuralClassifierTest {

    private lateinit var mockClassifier: SenseAiNeuralClassifier

    @Before
    fun setUp() {
        mockClassifier = mockk()
    }

    @Test
    fun classify_whenConfident_returnsPredictedIntent() {
        every { mockClassifier.isReady() } returns true
        every { mockClassifier.classify("Có nguy hiểm không?") } returns NeuralIntentPrediction(
            intent = DialogueIntent.SAFETY_CHECK,
            confidence = 0.92f,
            rawTag = "SAFETY_CHECK"
        )

        val model = SenseAiDialogueModel(neuralClassifier = mockClassifier)
        val intent = model.detectIntent("Có nguy hiểm không?")

        assertEquals(DialogueIntent.SAFETY_CHECK, intent)
    }

    @Test
    fun classify_whenLowConfidence_fallsBackToRuleIntent() {
        every { mockClassifier.isReady() } returns true
        every { mockClassifier.classify("Xin chào") } returns NeuralIntentPrediction(
            intent = DialogueIntent.GENERAL,
            confidence = 0.25f,
            rawTag = "GENERAL"
        )

        val model = SenseAiDialogueModel(neuralClassifier = mockClassifier)
        val intent = model.detectIntent("Xin chào")

        assertEquals(DialogueIntent.GREETING, intent)
    }

    @Test
    fun classify_whenClassifierNotReady_fallsBackToRuleIntent() {
        every { mockClassifier.isReady() } returns false

        val model = SenseAiDialogueModel(neuralClassifier = mockClassifier)
        val intent = model.detectIntent("Đọc biển báo giúp tôi")

        assertEquals(DialogueIntent.READ_TEXT, intent)
    }
}
