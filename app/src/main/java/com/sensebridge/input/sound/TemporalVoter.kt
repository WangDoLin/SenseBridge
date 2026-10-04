package com.sensebridge.input.sound

/**
 * Temporal k-of-n voting per sound group.
 *
 * Hypothesis: a single misclassified window is random, while a real horn/siren/alarm persists
 * across consecutive windows. A group is confirmed when it passed in at least [requiredVotes]
 * of the last [windowCount] windows, or immediately when one window is very confident.
 */
class TemporalVoter(
    private val windowCount: Int = DEFAULT_WINDOW_COUNT,
    private val requiredVotes: Int = DEFAULT_REQUIRED_VOTES
) {
    companion object {
        const val DEFAULT_WINDOW_COUNT = 3
        const val DEFAULT_REQUIRED_VOTES = 2
    }

    private val history = ArrayDeque<Set<SoundGroup>>()

    /**
     * Records one inference window and returns groups confirmed by this window.
     *
     * @param groupScores per-group scores of the current window.
     */
    fun submit(groupScores: Map<SoundGroup, Float>): Map<SoundGroup, Float> {
        val passing = AudioLabelTaxonomy.passingGroups(groupScores)
        history.addLast(passing.keys)
        while (history.size > windowCount) history.removeFirst()

        return passing.filter { (group, score) ->
            val isStrong = score >= group.threshold + SoundGroup.STRONG_SCORE_MARGIN
            isStrong || history.count { group in it } >= requiredVotes
        }
    }

    fun reset() = history.clear()
}
