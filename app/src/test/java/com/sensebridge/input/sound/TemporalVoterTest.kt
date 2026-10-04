package com.sensebridge.input.sound

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TemporalVoterTest {

    private lateinit var voter: TemporalVoter

    @Before
    fun setUp() {
        voter = TemporalVoter(windowCount = 3, requiredVotes = 2)
    }

    @Test
    fun singleWeakWindow_isNotConfirmed() {
        val confirmed = voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN))
        assertFalse(SoundGroup.CAR_HORN in confirmed)
    }

    @Test
    fun twoWeakWindows_areConfirmed() {
        voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN))
        val confirmed = voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN))
        assertTrue(SoundGroup.CAR_HORN in confirmed)
    }

    @Test
    fun strongSingleWindow_isConfirmedImmediately() {
        val confirmed = voter.submit(mapOf(SoundGroup.CAR_HORN to STRONG_HORN))
        assertTrue(SoundGroup.CAR_HORN in confirmed)
    }

    @Test
    fun votesOutsideWindow_expire() {
        voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN))
        voter.submit(emptyMap())
        voter.submit(emptyMap())
        val confirmed = voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN))
        assertFalse(SoundGroup.CAR_HORN in confirmed)
    }

    @Test
    fun belowThresholdScores_neverVote() {
        repeat(REPEAT_COUNT) { voter.submit(mapOf(SoundGroup.CAR_HORN to BELOW_THRESHOLD)) }
        val confirmed = voter.submit(mapOf(SoundGroup.CAR_HORN to BELOW_THRESHOLD))
        assertTrue(confirmed.isEmpty())
    }

    @Test
    fun continuousHorn_isConfirmedEveryWindow() {
        voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN))
        repeat(REPEAT_COUNT) {
            assertTrue(SoundGroup.CAR_HORN in voter.submit(mapOf(SoundGroup.CAR_HORN to WEAK_HORN)))
        }
    }

    private companion object {
        val WEAK_HORN = SoundGroup.CAR_HORN.threshold + 0.05f
        val STRONG_HORN = SoundGroup.CAR_HORN.threshold + SoundGroup.STRONG_SCORE_MARGIN + 0.05f
        val BELOW_THRESHOLD = SoundGroup.CAR_HORN.threshold - 0.05f
        const val REPEAT_COUNT = 5
    }
}
