package com.sensebridge.input.vision

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optical Character Recognition (OCR) engine powered by Google ML Kit.
 */
@Singleton
class OcrEngine @Inject constructor(
    private val eventEngine: EventEngine
) {
    companion object {
        private const val TAG = "OcrEngine"
        private const val MIN_TEXT_LENGTH = 2
    }

    private var _recognizer: TextRecognizer? = null
    private fun getRecognizer(): TextRecognizer {
        return _recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also {
            _recognizer = it
        }
    }

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    fun processImageProxy(
        imageProxy: ImageProxy,
        onComplete: (String?) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onComplete(null)
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        getRecognizer().process(image)
            .addOnSuccessListener { visionText ->
                val cleanedText = cleanRecognizedText(visionText.text)
                if (cleanedText.isNotBlank()) {
                    eventEngine.submitEvent(
                        SenseEvent(
                            source = SensorySource.VISION_OCR,
                            label = "ocr_text",
                            displayTitle = "Văn bản nhận diện",
                            spokenText = cleanedText,
                            confidence = 0.95f,
                            priority = PriorityLevel.ATTENTION_P2
                        )
                    )
                    onComplete(cleanedText)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR recognition failed: ${e.message}", e)
                onComplete(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Filters out OCR noise and short stray artifacts.
     */
    fun cleanRecognizedText(rawText: String): String {
        return rawText.lines()
            .map { it.trim() }
            .filter { it.length >= MIN_TEXT_LENGTH }
            .joinToString(separator = ", ")
    }

    fun release() {
        _recognizer?.close()
        _recognizer = null
    }
}
