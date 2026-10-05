package com.sensebridge.input.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrTextSanitizerTest {

    @Test
    fun sanitizeLine_decorativeHashes_areRemoved() {
        assertEquals("PHÒNG KHÁM SỐ 3", OcrTextSanitizer.sanitizeLine("## PHÒNG KHÁM ## SỐ 3 ##"))
    }

    @Test
    fun sanitizeLine_symbolOnlyLines_areDropped() {
        listOf("##", "####", "***", "|||", "~~~ ---", "#@!%^&a", ". . .", "= = =").forEach {
            assertNull("Expected noise for '$it'", OcrTextSanitizer.sanitizeLine(it))
        }
    }

    @Test
    fun sanitizeLine_meaningfulPunctuation_isKept() {
        assertEquals("Giờ mở cửa: 7:30 - 17:00", OcrTextSanitizer.sanitizeLine("Giờ mở cửa: 7:30 - 17:00"))
        assertEquals("Giá 50.000₫", OcrTextSanitizer.sanitizeLine("Giá 50.000₫"))
        assertEquals("Giảm 20%", OcrTextSanitizer.sanitizeLine("Giảm 20%"))
        assertEquals("(028) 3456-7890", OcrTextSanitizer.sanitizeLine("(028) 3456-7890"))
    }

    @Test
    fun sanitizeLine_repeatedPunctuationAndTypographicQuotes_areNormalized() {
        assertEquals("Lối ra.", OcrTextSanitizer.sanitizeLine("Lối ra......"))
        assertEquals("Phòng \"Cấp cứu\"", OcrTextSanitizer.sanitizeLine("Phòng “Cấp cứu”"))
    }

    @Test
    fun sanitizeLine_decomposedVietnamese_isRecomposed() {
        val decomposed = java.text.Normalizer.normalize("TẦNG 2", java.text.Normalizer.Form.NFD)
        assertEquals("TẦNG 2", OcrTextSanitizer.sanitizeLine(decomposed))
    }

    @Test
    fun sanitizeLine_gibberishTokens_areDropped() {
        assertEquals("KHOA NỘI", OcrTextSanitizer.sanitizeLine("KHOA l1l1Il NỘI"))
        assertEquals("Lối đi", OcrTextSanitizer.sanitizeLine("Lối xkcdfgh đi"))
    }

    @Test
    fun sanitizeLine_acronymsAndRoomCodes_areKept() {
        assertEquals("Quầy BHYT A12B", OcrTextSanitizer.sanitizeLine("Quầy BHYT A12B"))
        assertEquals("CCCD", OcrTextSanitizer.sanitizeLine("CCCD"))
    }

    @Test
    fun sanitizeLine_chainsOfSingleCharacters_areDropped() {
        assertNull(OcrTextSanitizer.sanitizeLine("l i I l"))
        assertNull(OcrTextSanitizer.sanitizeLine("1"))
    }

    @Test
    fun composeReadableText_lowConfidenceAndTinyLines_areFiltered() {
        val lines = listOf(
            RecognizedLine("NHÀ THUỐC", confidence = 0.92f, heightPx = 80, blockIndex = 0),
            RecognizedLine("WvXx rnm", confidence = 0.2f, heightPx = 40, blockIndex = 0),
            RecognizedLine("tiny print", confidence = 0.9f, heightPx = 6, blockIndex = 0)
        )
        assertEquals("NHÀ THUỐC", OcrTextSanitizer.composeReadableText(lines))
    }

    @Test
    fun composeReadableText_separatorsFollowLayout() {
        val lines = listOf(
            RecognizedLine("PHÒNG KHÁM SỐ 3", blockIndex = 0),
            RecognizedLine("KHOA TAI MŨI HỌNG", blockIndex = 0),
            RecognizedLine("Vui lòng xếp hàng", blockIndex = 1),
            RecognizedLine("theo thứ tự", blockIndex = 1)
        )
        assertEquals(
            "PHÒNG KHÁM SỐ 3, KHOA TAI MŨI HỌNG. Vui lòng xếp hàng theo thứ tự",
            OcrTextSanitizer.composeReadableText(lines)
        )
    }

    @Test
    fun composeReadableText_consecutiveDuplicates_areMerged() {
        val lines = listOf(RecognizedLine("LỐI THOÁT"), RecognizedLine("lối thoát"), RecognizedLine("##"))
        assertEquals("LỐI THOÁT", OcrTextSanitizer.composeReadableText(lines))
    }
}
