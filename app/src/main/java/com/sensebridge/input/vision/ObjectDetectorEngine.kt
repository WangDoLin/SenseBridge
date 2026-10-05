package com.sensebridge.input.vision

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.core.model.SensorySource
import com.sensebridge.core.model.SpatialDirection
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent real-time object detection analyzer with:
 * 1. Anti-Spam relevance filtering (suppresses background clutter & repetitive generic objects).
 * 2. Dominant-obstacle selection (picks the most safety-critical object in the user's path).
 * 3. Open-Vocabulary Grounding integration (NVIDIA LocateAnything architecture).
 */
@Singleton
class ObjectDetectorEngine @Inject constructor(
    private val throttle: FrameRateThrottle,
    private val spatialContextEngine: SpatialContextEngine,
    private val sentenceGenerator: SentenceGenerator,
    private val locateEngine: LocateGroundingEngine,
    private val eventEngine: EventEngine
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ObjectDetectorEngine"
        private const val DEFAULT_CONFIDENCE = 0.80f
        private const val MIN_AREA_RATIO = 0.06f // Ignore objects occupying < 6% of screen
        private const val MIN_OBSTACLE_AREA_RATIO = 0.22f // Threshold for generic obstacle
        private const val OBJECT_ANNOUNCE_COOLDOWN_MS = 6000L // 6s cooldown per object type
        private const val QUARTER_TURN_DEGREES = 90
        private const val THREE_QUARTER_TURN_DEGREES = 270
        private const val MIN_STABLE_FRAMES = 2
    }

    private var detector: ObjectDetector? = null
    private val recentAnnounceTimes = ConcurrentHashMap<String, Long>()
    private val trackedFrameCounts = ConcurrentHashMap<String, Int>()

    private fun getOrCreateDetector(): ObjectDetector {
        val current = detector
        if (current != null) return current

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()

        return ObjectDetection.getClient(options).also {
            detector = it
        }
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

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        // ML Kit returns boxes in the UPRIGHT image; in portrait the sensor frame is rotated 90°,
        // so width/height must be swapped or LEFT/RIGHT are computed against the wrong axis.
        val isRotatedQuarter = rotation == QUARTER_TURN_DEGREES || rotation == THREE_QUARTER_TURN_DEGREES
        val frameWidth = if (isRotatedQuarter) imageProxy.height else imageProxy.width
        val frameHeight = if (isRotatedQuarter) imageProxy.width else imageProxy.height
        val now = System.currentTimeMillis()

        getOrCreateDetector().process(image)
            .addOnSuccessListener { detectedObjects ->
                processDetections(detectedObjects, frameWidth, frameHeight, now)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Object detection error: ${e.message}", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
                throttle.onFrameCompleted()
            }
    }

    private fun processDetections(
        detectedObjects: List<DetectedObject>,
        frameWidth: Int,
        frameHeight: Int,
        now: Long
    ) {
        if (detectedObjects.isEmpty()) return

        // 1. Check if user has an active NVIDIA Locate target search
        val activeQuery = locateEngine.activeSearchQuery.value
        if (!activeQuery.isNullOrBlank()) {
            for (obj in detectedObjects) {
                val primaryLabel = obj.labels.maxByOrNull { it.confidence }?.text ?: "object"
                val confidence = obj.labels.maxByOrNull { it.confidence }?.confidence ?: DEFAULT_CONFIDENCE
                val target = locateEngine.matchAndGround(primaryLabel, obj.boundingBox, frameWidth, frameHeight, confidence)

                if (target != null) {
                    val key = "LOCATE_${target.label}_${target.spatialDirection}"
                    val lastTime = recentAnnounceTimes[key] ?: 0L
                    if (now - lastTime > 3500L) {
                        recentAnnounceTimes[key] = now
                        eventEngine.submitEvent(
                            SenseEvent(
                                source = SensorySource.VISION_OBJECT_DETECTOR,
                                label = "locate_${target.label}",
                                displayTitle = "Đã tìm thấy: ${target.vietnameseName}",
                                spokenText = target.guidanceInstruction,
                                confidence = target.confidence,
                                priority = PriorityLevel.ATTENTION_P2,
                                spatialDirection = target.spatialDirection,
                                trackingId = obj.trackingId
                            )
                        )
                    }
                    return // Found searched target, don't spam other background objects
                }
            }
        }

        // 2. Anti-Spam Filtered Standard Mode: Pick the SINGLE MOST CRITICAL object in the frame
        var mostCriticalCandidate: CandidateEvent? = null
        val keysSeenThisFrame = mutableSetOf<String>()

        for (obj in detectedObjects) {
            val context = spatialContextEngine.analyze(obj.boundingBox, frameWidth, frameHeight)

            // Ignore tiny clutter (< 6% screen area)
            if (context.areaRatio < MIN_AREA_RATIO) continue

            val primaryLabel = obj.labels.maxByOrNull { it.confidence }
            val rawLabel = primaryLabel?.text?.lowercase() ?: "object"
            val confidence = primaryLabel?.confidence ?: DEFAULT_CONFIDENCE

            // Filter generic unlabeled "object": only keep if it is a major obstacle in direct center path
            if (rawLabel == "object") {
                if (context.direction != SpatialDirection.CENTER || context.areaRatio < MIN_OBSTACLE_AREA_RATIO) {
                    continue // Skip generic object spam
                }
            }

            val labelToUse = if (rawLabel == "object") "chướng ngại vật" else rawLabel
            val description = sentenceGenerator.generateDescription(labelToUse, context)

            // Temporal stability filter: must appear across at least 2 frames
            val objKey = "${labelToUse}_${context.direction}_${obj.trackingId ?: 0}"
            keysSeenThisFrame.add(objKey)
            val count = (trackedFrameCounts[objKey] ?: 0) + 1
            trackedFrameCounts[objKey] = count
            if (count < MIN_STABLE_FRAMES) continue

            // Candidate scoring: Hazards (P1) > Near Objects (P2) > Far (P3)
            val score = calculateSignificanceScore(description.priority, context.areaRatio, context.direction)
            if (mostCriticalCandidate == null || score > mostCriticalCandidate.score) {
                mostCriticalCandidate = CandidateEvent(
                    key = "${labelToUse}_${context.direction}",
                    event = SenseEvent(
                        source = SensorySource.VISION_OBJECT_DETECTOR,
                        label = labelToUse,
                        displayTitle = description.vietnameseTitle,
                        spokenText = description.spokenSentence,
                        confidence = confidence,
                        priority = description.priority,
                        spatialDirection = context.direction,
                        trackingId = obj.trackingId
                    ),
                    score = score
                )
            }
        }

        // Objects that vanished must re-earn stability; also prevents unbounded map growth
        trackedFrameCounts.keys.retainAll(keysSeenThisFrame)

        // 3. Dispatch the single most meaningful event if outside cooldown
        if (mostCriticalCandidate != null) {
            val lastTime = recentAnnounceTimes[mostCriticalCandidate.key] ?: 0L
            val cooldown = if (mostCriticalCandidate.event.priority == PriorityLevel.WARNING_P1) 2500L else OBJECT_ANNOUNCE_COOLDOWN_MS

            if (now - lastTime >= cooldown) {
                recentAnnounceTimes[mostCriticalCandidate.key] = now
                eventEngine.submitEvent(mostCriticalCandidate.event)
            }
        }
    }

    private fun calculateSignificanceScore(priority: PriorityLevel, areaRatio: Float, direction: SpatialDirection): Float {
        val priorityScore = when (priority) {
            PriorityLevel.CRITICAL_P0 -> 100f
            PriorityLevel.WARNING_P1 -> 50f
            PriorityLevel.ATTENTION_P2 -> 20f
            PriorityLevel.INFO_P3 -> 5f
        }
        val centerBonus = if (direction == SpatialDirection.CENTER) 15f else 0f
        return priorityScore + (areaRatio * 30f) + centerBonus
    }

    private data class CandidateEvent(
        val key: String,
        val event: SenseEvent,
        val score: Float
    )

    fun release() {
        try {
            detector?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ObjectDetector: ${e.message}", e)
        }
        detector = null
        throttle.reset()
        recentAnnounceTimes.clear()
        trackedFrameCounts.clear()
    }
}
