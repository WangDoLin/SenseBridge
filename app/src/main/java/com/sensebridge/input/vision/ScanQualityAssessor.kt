package com.sensebridge.input.vision

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Outcome of visual scan quality assessment.
 */
enum class ScanQualityStatus(val messageVi: String) {
    OPTIMAL("Chất lượng ảnh tốt, sẵn sàng nhận diện"),
    BLURRED("Ảnh bị rung mờ, vui lòng giữ yên thiết bị"),
    TOO_DARK("Môi trường quá tối, vui lòng tăng ánh sáng hoặc bật đèn"),
    TOO_BRIGHT("Ảnh bị chói sáng, vui lòng điều chỉnh góc nghiêng"),
    INSUFFICIENT_CONTRAST("Độ tương phản thấp, khó phân biệt văn bản")
}

data class ScanQualityReport(
    val status: ScanQualityStatus,
    val isAcceptableForOcr: Boolean,
    val blurScore: Double,
    val averageLuminance: Double,
    val contrastRatio: Double
)

/**
 * Real-time image quality evaluator for assistive camera scanning.
 * Employs lightweight pixel-grid sampling and discrete gradient variance to detect
 * hand tremor motion blur, poor illumination, and specular glare on mobile devices.
 */
object ScanQualityAssessor {

    /**
     * Threshold for Laplacian / discrete gradient variance.
     * Below this value, hand shake or motion blur degrades OCR character segmentation.
     */
    const val BLUR_VARIANCE_THRESHOLD = 85.0

    /** Minimum average luminance (0-255) to consider illumination sufficient. */
    const val MIN_LUMINANCE_THRESHOLD = 38.0

    /** Maximum average luminance (0-255) above which specular overexposure occurs. */
    const val MAX_LUMINANCE_THRESHOLD = 230.0

    /** Minimum contrast ratio (max - min luminance) / 255. */
    const val MIN_CONTRAST_RATIO = 0.22

    private const val SAMPLE_GRID_STEP = 4 // Downsample grid step for sub-millisecond mobile latency

    /**
     * Evaluates a camera bitmap frame and produces a comprehensive quality report.
     */
    fun evaluate(bitmap: Bitmap): ScanQualityReport {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            return ScanQualityReport(
                status = ScanQualityStatus.BLURRED,
                isAcceptableForOcr = false,
                blurScore = 0.0,
                averageLuminance = 0.0,
                contrastRatio = 0.0
            )
        }

        var totalLuminance = 0.0
        var minLum = 255.0
        var maxLum = 0.0
        var sampleCount = 0

        // Sub-sample luminance grid for fast mobile execution
        val stepX = max(1, width / 64)
        val stepY = max(1, height / 64)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // ITU-R BT.601 standard luminance calculation
                val lum = 0.299 * r + 0.587 * g + 0.114 * b

                totalLuminance += lum
                minLum = min(minLum, lum)
                maxLum = max(maxLum, lum)
                sampleCount++
            }
        }

        val avgLuminance = if (sampleCount > 0) totalLuminance / sampleCount else 0.0
        val contrastRatio = if (maxLum > minLum) (maxLum - minLum) / 255.0 else 0.0

        // Motion blur estimation via discrete gradient variance on the central ROI
        val blurScore = estimateSharpnessVariance(bitmap)

        val status = when {
            avgLuminance < MIN_LUMINANCE_THRESHOLD -> ScanQualityStatus.TOO_DARK
            avgLuminance > MAX_LUMINANCE_THRESHOLD -> ScanQualityStatus.TOO_BRIGHT
            contrastRatio < MIN_CONTRAST_RATIO -> ScanQualityStatus.INSUFFICIENT_CONTRAST
            blurScore < BLUR_VARIANCE_THRESHOLD -> ScanQualityStatus.BLURRED
            else -> ScanQualityStatus.OPTIMAL
        }

        val isAcceptable = status == ScanQualityStatus.OPTIMAL ||
            (status == ScanQualityStatus.BLURRED && blurScore >= BLUR_VARIANCE_THRESHOLD * 0.75)

        return ScanQualityReport(
            status = status,
            isAcceptableForOcr = isAcceptable,
            blurScore = blurScore,
            averageLuminance = avgLuminance,
            contrastRatio = contrastRatio
        )
    }

    /**
     * Computes discrete gradient energy variance over the center 50% ROI of the frame.
     * High gradient variance indicates sharp textual edges; low variance indicates defocus or blur.
     */
    fun estimateSharpnessVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 8 || height < 8) return 0.0

        val startX = width / 4
        val endX = (width * 3) / 4
        val startY = height / 4
        val endY = (height * 3) / 4

        val gradients = mutableListOf<Double>()
        var gradientSum = 0.0

        val step = max(2, min(width, height) / 48)

        for (y in startY until endY - step step step) {
            for (x in startX until endX - step step step) {
                val pCenter = bitmap.getPixel(x, y)
                val pRight = bitmap.getPixel(x + step, y)
                val pDown = bitmap.getPixel(x, y + step)

                val lumC = 0.299 * Color.red(pCenter) + 0.587 * Color.green(pCenter) + 0.114 * Color.blue(pCenter)
                val lumR = 0.299 * Color.red(pRight) + 0.587 * Color.green(pRight) + 0.114 * Color.blue(pRight)
                val lumD = 0.299 * Color.red(pDown) + 0.587 * Color.green(pDown) + 0.114 * Color.blue(pDown)

                val dx = abs(lumR - lumC)
                val dy = abs(lumD - lumC)
                val gradMag = dx + dy

                gradients.add(gradMag)
                gradientSum += gradMag
            }
        }

        if (gradients.size < 4) return 0.0

        val mean = gradientSum / gradients.size
        var varianceSum = 0.0
        for (g in gradients) {
            val diff = g - mean
            varianceSum += diff * diff
        }

        return varianceSum / gradients.size
    }
}
