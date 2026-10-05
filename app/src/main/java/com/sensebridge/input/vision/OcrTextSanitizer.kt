package com.sensebridge.input.vision

import java.text.Normalizer

data class RecognizedLine(
    val text: String,
    val confidence: Float = UNKNOWN_CONFIDENCE,
    val heightPx: Int = UNKNOWN_HEIGHT,
    val blockIndex: Int = 0
) {
    companion object {
        const val UNKNOWN_CONFIDENCE = 0f
        const val UNKNOWN_HEIGHT = 0
    }
}

object OcrTextSanitizer {
    const val MIN_LINE_CONFIDENCE = 0.4f
    const val MIN_LINE_HEIGHT_PX = 12

    private const val MIN_ALNUM_CHARS = 2
    private const val MIN_WORD_ALNUM_CHARS = 2
    private const val MIN_ALNUM_RATIO = 0.5f
    private const val MAX_LETTER_DIGIT_SWITCHES = 2
    private const val MIN_VOWELLESS_GIBBERISH_LENGTH = 6

    private const val LINE_SEPARATOR = ", "
    private const val BLOCK_SEPARATOR = ". "
    private const val CONTINUATION_SEPARATOR = " "

    private const val MEANINGFUL_PUNCTUATION = ".,:;!?%/-()&+'\"°₫$"
    private const val SENTENCE_END_PUNCTUATION = ".!?:;"
    private const val ALLOWED_LEADING_SYMBOLS = "(\"'$"
    private const val ALLOWED_TRAILING_SYMBOLS = ".!?%)\"'°₫"
    private const val VOWELS = "aeiouy"

    private val CONNECTOR_TOKENS = setOf("-", "/", "&", "+")

    private val TYPOGRAPHIC_REPLACEMENTS = mapOf(
        '‘' to '\'', '’' to '\'', '“' to '"', '”' to '"',
        '–' to '-', '—' to '-', '…' to '.', '·' to '.', '•' to ' '
    )
    private val WHITESPACE = Regex("\\s+")
    private val REPEATED_PUNCTUATION = Regex("(\\p{Punct})\\1+")
    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    fun sanitizeText(rawText: String): String =
        composeReadableText(rawText.lines().map { RecognizedLine(text = it) })

    fun composeReadableText(lines: List<RecognizedLine>): String {
        val segments = lines.asSequence()
            .filter(::isReliable)
            .mapNotNull { line -> sanitizeLine(line.text)?.let { line.blockIndex to it } }
            .toList()
        return joinSegments(removeConsecutiveDuplicates(segments))
    }

    fun sanitizeLine(rawLine: String): String? {
        val collapsed = REPEATED_PUNCTUATION.replace(normalize(rawLine), "$1")
        if (alnumRatio(collapsed) < MIN_ALNUM_RATIO) return null

        val tokens = keepAllowedCharacters(collapsed).split(WHITESPACE).filterNot(::isNoiseToken)
        if (tokens.none { alnumCount(it) >= MIN_WORD_ALNUM_CHARS }) return null

        val cleaned = trimEdges(tokens.joinToString(" "))
        return cleaned.takeIf { alnumCount(it) >= MIN_ALNUM_CHARS }
    }

    private fun isReliable(line: RecognizedLine): Boolean {
        val confidenceOk = line.confidence <= RecognizedLine.UNKNOWN_CONFIDENCE ||
            line.confidence >= MIN_LINE_CONFIDENCE
        val heightOk = line.heightPx <= RecognizedLine.UNKNOWN_HEIGHT ||
            line.heightPx >= MIN_LINE_HEIGHT_PX
        return confidenceOk && heightOk
    }

    private fun normalize(text: String): String {
        val composed = Normalizer.normalize(text, Normalizer.Form.NFC)
        return composed.map { TYPOGRAPHIC_REPLACEMENTS[it] ?: it }.joinToString("")
    }

    private fun keepAllowedCharacters(text: String): String {
        val builder = StringBuilder(text.length)
        for (ch in text) {
            when {
                ch.isLetterOrDigit() || ch in MEANINGFUL_PUNCTUATION -> builder.append(ch)
                Character.getType(ch) == Character.NON_SPACING_MARK.toInt() ->
                    if (builder.lastOrNull()?.isLetter() == true) builder.append(ch)
                else -> builder.append(' ')
            }
        }
        return builder.toString().trim()
    }

    private fun isNoiseToken(token: String): Boolean {
        val core = token.filter { it.isLetterOrDigit() }
        if (core.isEmpty()) return token !in CONNECTOR_TOKENS
        return countLetterDigitSwitches(core) > MAX_LETTER_DIGIT_SWITCHES || isVowellessGibberish(core)
    }

    private fun countLetterDigitSwitches(core: String): Int =
        core.zipWithNext().count { (a, b) -> a.isDigit() != b.isDigit() }

    private fun isVowellessGibberish(core: String): Boolean {
        if (core.length < MIN_VOWELLESS_GIBBERISH_LENGTH) return false
        if (!core.all { it.isLetter() } || core.none { it.isLowerCase() }) return false
        val baseLetters = COMBINING_MARKS.replace(Normalizer.normalize(core, Normalizer.Form.NFD), "")
        return baseLetters.lowercase().none { it in VOWELS }
    }

    private fun trimEdges(text: String): String = text
        .trimStart { !it.isLetterOrDigit() && it !in ALLOWED_LEADING_SYMBOLS }
        .trimEnd { !it.isLetterOrDigit() && it !in ALLOWED_TRAILING_SYMBOLS }

    private fun alnumCount(text: String): Int = text.count { it.isLetterOrDigit() }

    private fun alnumRatio(text: String): Float {
        val visible = text.count { !it.isWhitespace() }
        return if (visible == 0) 0f else alnumCount(text).toFloat() / visible
    }

    private fun removeConsecutiveDuplicates(segments: List<Pair<Int, String>>): List<Pair<Int, String>> =
        segments.filterIndexed { index, (_, text) ->
            index == 0 || !segments[index - 1].second.equals(text, ignoreCase = true)
        }

    private fun joinSegments(segments: List<Pair<Int, String>>): String {
        val builder = StringBuilder()
        segments.forEachIndexed { index, (blockIndex, text) ->
            if (index > 0) {
                val (previousBlock, previousText) = segments[index - 1]
                builder.append(chooseSeparator(previousText, text, previousBlock != blockIndex))
            }
            builder.append(text)
        }
        return builder.toString()
    }

    private fun chooseSeparator(previous: String, next: String, isNewBlock: Boolean): String = when {
        previous.last() in SENTENCE_END_PUNCTUATION -> CONTINUATION_SEPARATOR
        !isNewBlock && next.first().isLowerCase() -> CONTINUATION_SEPARATOR
        isNewBlock -> BLOCK_SEPARATOR
        else -> LINE_SEPARATOR
    }
}
