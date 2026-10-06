package com.sensebridge.input.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartDocumentParserTest {

    @Test
    fun parse_vietnameseCurrency_correctlyIdentifiesDenomination() {
        val result500k = SmartDocumentParser.parse("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM 500.000 ĐỒNG")
        assertEquals(ScannedDomain.CURRENCY, result500k.domain)
        assertEquals("năm trăm nghìn đồng", result500k.primaryEntity)
        assertTrue(result500k.speechSummary.contains("năm trăm nghìn đồng"))

        val result20k = SmartDocumentParser.parse("Ngân hàng Nhà nước Việt Nam 20.000 VND")
        assertEquals(ScannedDomain.CURRENCY, result20k.domain)
        assertEquals("hai mươi nghìn đồng", result20k.primaryEntity)
    }

    @Test
    fun parse_medicationWithDosage_extractsMedicineAndDosageDetails() {
        val input = "PANADOL EXTRA PARACETAMOL 500 MG UỐNG NGÀY 2 LẦN LẦN 1 VIÊN"
        val result = SmartDocumentParser.parse(input)

        assertEquals(ScannedDomain.MEDICATION, result.domain)
        assertNotNull(result.primaryEntity)
        assertTrue(result.speechSummary.contains("thuốc"))
        assertTrue(result.details.isNotEmpty())
    }

    @Test
    fun parse_busRouteSign_identifiesPublicTransport() {
        val input = "TRẠM DỪNG XE BUÝT TUYẾN SỐ 150 BẾN XE MIỀN ĐÔNG"
        val result = SmartDocumentParser.parse(input)

        assertEquals(ScannedDomain.TRANSPORTATION_BUS, result.domain)
        assertEquals("Xe buýt 150", result.primaryEntity)
        assertTrue(result.speechSummary.contains("150"))
    }

    @Test
    fun parse_emergencySignage_identifiesFacilitySign() {
        val input = "LỐI THOÁT HIỂM KHẨN CẤP EMERGENCY EXIT TẦNG 2"
        val result = SmartDocumentParser.parse(input)

        assertEquals(ScannedDomain.FACILITY_SIGNAGE, result.domain)
        assertTrue(result.speechSummary.contains("Biển chỉ dẫn"))
    }

    @Test
    fun parse_generalText_fallsBackToCleanText() {
        val input = "Chào mừng quý khách đến với hội thảo tiếp cận số"
        val result = SmartDocumentParser.parse(input)

        assertEquals(ScannedDomain.GENERAL_TEXT, result.domain)
        assertEquals(input, result.speechSummary)
    }
}
