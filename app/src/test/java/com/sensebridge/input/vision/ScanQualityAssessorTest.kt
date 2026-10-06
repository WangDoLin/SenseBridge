package com.sensebridge.input.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanQualityAssessorTest {

    @Test
    fun scanQualityStatus_hasDescriptiveVietnameseMessages() {
        assertEquals("Chất lượng ảnh tốt, sẵn sàng nhận diện", ScanQualityStatus.OPTIMAL.messageVi)
        assertEquals("Ảnh bị rung mờ, vui lòng giữ yên thiết bị", ScanQualityStatus.BLURRED.messageVi)
        assertEquals("Môi trường quá tối, vui lòng tăng ánh sáng hoặc bật đèn", ScanQualityStatus.TOO_DARK.messageVi)
        assertEquals("Ảnh bị chói sáng, vui lòng điều chỉnh góc nghiêng", ScanQualityStatus.TOO_BRIGHT.messageVi)
        assertEquals("Độ tương phản thấp, khó phân biệt văn bản", ScanQualityStatus.INSUFFICIENT_CONTRAST.messageVi)
    }

    @Test
    fun scanQualityConstants_conformToScientificCalibration() {
        assertTrue(ScanQualityAssessor.BLUR_VARIANCE_THRESHOLD > 0)
        assertTrue(ScanQualityAssessor.MIN_LUMINANCE_THRESHOLD in 20.0..60.0)
        assertTrue(ScanQualityAssessor.MAX_LUMINANCE_THRESHOLD in 200.0..250.0)
        assertTrue(ScanQualityAssessor.MIN_CONTRAST_RATIO in 0.1..0.4)
    }

    @Test
    fun scanQualityReport_dataStructure_storesExpectedFields() {
        val report = ScanQualityReport(
            status = ScanQualityStatus.OPTIMAL,
            isAcceptableForOcr = true,
            blurScore = 142.5,
            averageLuminance = 128.0,
            contrastRatio = 0.65
        )

        assertEquals(ScanQualityStatus.OPTIMAL, report.status)
        assertTrue(report.isAcceptableForOcr)
        assertEquals(142.5, report.blurScore, 0.01)
        assertEquals(128.0, report.averageLuminance, 0.01)
        assertEquals(0.65, report.contrastRatio, 0.01)
    }
}
