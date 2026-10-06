package com.sensebridge.input.vision

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrEngine @Inject constructor(
    private val eventEngine: EventEngine
) {
    companion object {
        private const val TAG = "OcrEngine"
        private const val EVENT_LABEL = "ocr_text"
        private const val EVENT_TITLE = "Văn bản nhận diện"
        private const val EVENT_CONFIDENCE = 0.95f
        private const val MIN_ELEMENT_CONFIDENCE = 0.3f
    }

    private var _recognizer: TextRecognizer? = null
    private fun getRecognizer(): TextRecognizer {
        return _recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also {
            _recognizer = it
        }
    }

    fun processImageProxy(
        imageProxy: ImageProxy,
        onComplete: (String?) -> Unit
    ) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val bitmap = decodeFrame(imageProxy)
        if (bitmap == null) {
            onComplete(null)
            return
        }

        getRecognizer().process(InputImage.fromBitmap(bitmap, rotationDegrees))
            .addOnSuccessListener { visionText -> handleRecognitionResult(visionText, onComplete) }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR recognition failed: ${e.message}", e)
                onComplete(null)
            }
            .addOnCompleteListener { bitmap.recycle() }
    }

    fun cleanRecognizedText(rawText: String): String = OcrTextSanitizer.sanitizeText(rawText)

    fun cleanRecognizedText(visionText: Text): String =
        OcrTextSanitizer.composeReadableText(extractLines(visionText))

    fun release() {
        _recognizer?.close()
        _recognizer = null
    }

    private fun decodeFrame(imageProxy: ImageProxy): Bitmap? = try {
        OcrImagePreprocessor.toViewportBitmap(imageProxy)
    } catch (e: UnsupportedOperationException) {
        Log.e(TAG, "Unsupported camera frame format for OCR: ${imageProxy.format}", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Invalid camera frame for OCR", e)
        null
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "Not enough memory to decode OCR frame", e)
        null
    } finally {
        imageProxy.close()
    }

    private fun handleRecognitionResult(visionText: Text, onComplete: (String?) -> Unit) {
        val cleanedText = cleanRecognizedText(visionText)
        if (cleanedText.isBlank()) {
            onComplete(null)
            return
        }

        val parsedResult = SmartDocumentParser.parse(cleanedText)
        val spokenMessage = if (parsedResult.domain != ScannedDomain.GENERAL_TEXT) {
            parsedResult.speechSummary
        } else {
            cleanedText
        }

        val displayTitle = when (parsedResult.domain) {
            ScannedDomain.MEDICATION -> "Thông tin thuốc"
            ScannedDomain.CURRENCY -> "Mệnh giá tiền"
            ScannedDomain.TRANSPORTATION_BUS -> "Tuyến xe buýt"
            ScannedDomain.FACILITY_SIGNAGE -> "Biển chỉ dẫn"
            ScannedDomain.RECEIPT_BILL -> "Hóa đơn"
            ScannedDomain.GENERAL_TEXT -> EVENT_TITLE
        }

        eventEngine.submitEvent(
            SenseEvent(
                source = SensorySource.VISION_OCR,
                label = EVENT_LABEL,
                displayTitle = displayTitle,
                spokenText = spokenMessage,
                confidence = EVENT_CONFIDENCE,
                priority = PriorityLevel.ATTENTION_P2
            )
        )
        onComplete(spokenMessage)
    }

    private fun extractLines(visionText: Text): List<RecognizedLine> =
        visionText.textBlocks.flatMapIndexed { blockIndex, block ->
            block.lines.map { line ->
                RecognizedLine(
                    text = buildConfidentLineText(line),
                    confidence = line.confidence,
                    heightPx = line.boundingBox?.height() ?: RecognizedLine.UNKNOWN_HEIGHT,
                    blockIndex = blockIndex
                )
            }
        }

    private fun buildConfidentLineText(line: Text.Line): String =
        line.elements
            .filter { it.confidence <= RecognizedLine.UNKNOWN_CONFIDENCE || it.confidence >= MIN_ELEMENT_CONFIDENCE }
            .joinToString(" ") { it.text }
}
