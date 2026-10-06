package com.sensebridge.input.vision

import java.util.regex.Pattern

/**
 * Domain category inferred from scanned text.
 */
enum class ScannedDomain {
    MEDICATION,
    CURRENCY,
    TRANSPORTATION_BUS,
    FACILITY_SIGNAGE,
    RECEIPT_BILL,
    GENERAL_TEXT
}

data class ParsedScanResult(
    val domain: ScannedDomain,
    val primaryEntity: String?,
    val speechSummary: String,
    val details: List<String> = emptyList()
)

/**
 * Semantic entity extractor for assistive scanning.
 * Analyzes OCR text streams to identify medications, Vietnamese currency denominations,
 * public transport bus routes, emergency facility signage, and billing amounts.
 */
object SmartDocumentParser {

    private val CURRENCY_REGEX = Pattern.compile(
        """(?i)(500[.,]000|200[.,]000|100[.,]000|50[.,]000|20[.,]000|10[.,]000|5[.,]000|2[.,]000|1[.,]000)\s*(đ|vnd|đồng)?"""
    )

    private val BUS_REGEX = Pattern.compile(
        """(?i)(xe\s*(bus|buýt)|tuyến\s*số?|bus\s*no\.?)\s*[:#-]?\s*([0-9]{1,3}[A-Z]?)"""
    )

    private val MEDICINE_KEYWORDS = listOf(
        "paracetamol", "panadol", "efferalgan", "amoxicillin", "augmentin",
        "ibuprofen", "aspirin", "hapacol", "decolgen", "berocca", "cefixim",
        "kháng sinh", "viên nén", "viên nang", "viên sủi", "siro", "thuốc"
    )

    private val DOSAGE_REGEX = Pattern.compile(
        """(?i)(\d+\s*(mg|ml|mcg|gam|g))|(\d+\s*viên)|(ngày\s*\d+\s*lần)|(lần\s*\d+\s*viên)|(uống\s*(trước|sau)\s*ăn)"""
    )

    private val SIGNAGE_KEYWORDS = listOf(
        "lối thoát hiểm", "exit", "nhà vệ sinh", "toilet", "wc", "cấm vào",
        "nguy hiểm", "bệnh viện", "nhà thuốc", "phòng cấp cứu", "tầng", "phòng"
    )

    private val BILL_KEYWORDS = listOf(
        "tổng cộng", "thành tiền", "thanh toán", "hóa đơn", "phiếu thu", "total"
    )

    /**
     * Parses raw OCR text into domain-specific structured information.
     */
    fun parse(rawText: String): ParsedScanResult {
        val lowerText = rawText.lowercase()

        // 1. Check Currency (Banknotes)
        val currencyMatch = CURRENCY_REGEX.matcher(lowerText)
        if (currencyMatch.find()) {
            val amount = currencyMatch.group(1)?.replace(".", "")?.replace(",", "") ?: ""
            val formatted = formatVietnameseCurrency(amount)
            return ParsedScanResult(
                domain = ScannedDomain.CURRENCY,
                primaryEntity = formatted,
                speechSummary = "Nhận diện tờ tiền mệnh giá $formatted.",
                details = listOf("Mệnh giá: $formatted")
            )
        }

        // 2. Check Bus / Public Transit
        val busMatch = BUS_REGEX.matcher(lowerText)
        if (busMatch.find()) {
            val route = busMatch.group(3) ?: ""
            return ParsedScanResult(
                domain = ScannedDomain.TRANSPORTATION_BUS,
                primaryEntity = "Xe buýt $route",
                speechSummary = "Phát hiện xe buýt tuyến số $route.",
                details = listOf("Tuyến xe: $route")
            )
        }

        // 3. Check Medication & Prescription
        val detectedMedicine = MEDICINE_KEYWORDS.firstOrNull { lowerText.contains(it) }
        val dosageMatches = mutableListOf<String>()
        val dosageMatcher = DOSAGE_REGEX.matcher(lowerText)
        while (dosageMatcher.find()) {
            dosageMatches.add(dosageMatcher.group())
        }

        if (detectedMedicine != null || (dosageMatches.size >= 2 && lowerText.contains("uống"))) {
            val medName = detectedMedicine?.replaceFirstChar { it.uppercase() } ?: "Đơn thuốc"
            val dosageSummary = if (dosageMatches.isNotEmpty()) {
                ", liều lượng: " + dosageMatches.take(3).joinToString(", ")
            } else ""

            return ParsedScanResult(
                domain = ScannedDomain.MEDICATION,
                primaryEntity = medName,
                speechSummary = "Nhận diện thông tin thuốc: $medName$dosageSummary.",
                details = dosageMatches
            )
        }

        // 4. Check Facility Signage
        val detectedSign = SIGNAGE_KEYWORDS.firstOrNull { lowerText.contains(it) }
        if (detectedSign != null) {
            val signName = detectedSign.replaceFirstChar { it.uppercase() }
            return ParsedScanResult(
                domain = ScannedDomain.FACILITY_SIGNAGE,
                primaryEntity = signName,
                speechSummary = "Biển chỉ dẫn nhận diện: $rawText.",
                details = listOf(rawText)
            )
        }

        // 5. Check Receipts & Invoices
        val isBill = BILL_KEYWORDS.any { lowerText.contains(it) }
        if (isBill) {
            return ParsedScanResult(
                domain = ScannedDomain.RECEIPT_BILL,
                primaryEntity = "Hóa đơn thanh toán",
                speechSummary = "Nhận diện hóa đơn hoặc phiếu thanh toán.",
                details = listOf(rawText)
            )
        }

        // 6. General Text
        return ParsedScanResult(
            domain = ScannedDomain.GENERAL_TEXT,
            primaryEntity = null,
            speechSummary = rawText,
            details = listOf(rawText)
        )
    }

    private fun formatVietnameseCurrency(rawDigits: String): String {
        return when (rawDigits) {
            "500000" -> "năm trăm nghìn đồng"
            "200000" -> "hai trăm nghìn đồng"
            "100000" -> "một trăm nghìn đồng"
            "50000" -> "năm mươi nghìn đồng"
            "20000" -> "hai mươi nghìn đồng"
            "10000" -> "mười nghìn đồng"
            "5000" -> "năm nghìn đồng"
            "2000" -> "hai nghìn đồng"
            "1000" -> "một nghìn đồng"
            else -> "$rawDigits đồng"
        }
    }
}
