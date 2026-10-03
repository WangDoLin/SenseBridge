package com.sensebridge.input.vision

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time object detection analyzer utilizing ML Kit in STREAM_MODE with tracking IDs.
 */
@Singleton
class ObjectDetectorEngine @Inject constructor(
    private val throttle: FrameRateThrottle,
    private val spatialContextEngine: SpatialContextEngine,
    private val sentenceGenerator: SentenceGenerator,
    private val eventEngine: EventEngine
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ObjectDetectorEngine"
        private const val DEFAULT_CONFIDENCE = 0.80f
    }

    private val detector: ObjectDetector

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()

        detector = ObjectDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!throttle.shouldProcessFrame()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            throttle.onFrameCompleted()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val frameWidth = imageProxy.width
        val frameHeight = imageProxy.height

        detector.process(image)
            .addOnSuccessListener { detectedObjects ->
                for (obj in detectedObjects) {
                    val primaryLabel = obj.labels.maxByOrNull { it.confidence }
                    val labelText = primaryLabel?.text ?: "object"
                    val confidence = primaryLabel?.confidence ?: DEFAULT_CONFIDENCE

                    // Spatial context
                    val context = spatialContextEngine.analyze(obj.boundingBox, frameWidth, frameHeight)
                    val description = sentenceGenerator.generateDescription(labelText, context)

                    eventEngine.submitEvent(
                        SenseEvent(
                            source = SensorySource.VISION_OBJECT_DETECTOR,
                            label = labelText.lowercase(),
                            displayTitle = description.vietnameseTitle,
                            spokenText = description.spokenSentence,
                            confidence = confidence,
                            priority = description.priority,
                            spatialDirection = context.direction,
                            trackingId = obj.trackingId
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Object detection error: ${e.message}", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
                throttle.onFrameCompleted()
            }
    }

    fun release() {
        detector.close()
        throttle.reset()
    }
}
