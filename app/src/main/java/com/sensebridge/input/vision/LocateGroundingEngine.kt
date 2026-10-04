package com.sensebridge.input.vision

import android.graphics.Rect
import android.util.Log
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of an open-vocabulary spatial grounding query inspired by NVIDIA LocateAnything.
 */
data class GroundingTarget(
    val query: String,
    val label: String,
    val vietnameseName: String,
    val box: Rect,
    val confidence: Float,
    val spatialDirection: SpatialDirection,
    val guidanceInstruction: String
)

/**
 * Open-Vocabulary Object Grounding Engine inspired by NVIDIA LocateAnything (Parallel Box Decoding).
 * Enables targeted locating of specific objects/items with precise directional guidance,
 * avoiding sensory noise and spam.
 */
@Singleton
class LocateGroundingEngine @Inject constructor(
    private val spatialContextEngine: SpatialContextEngine
) {
    companion object {
        private const val TAG = "LocateGroundingEngine"
    }

    private val _activeSearchQuery = MutableStateFlow<String?>(null)
    val activeSearchQuery: StateFlow<String?> = _activeSearchQuery.asStateFlow()

    private val _lastFoundTarget = MutableStateFlow<GroundingTarget?>(null)
    val lastFoundTarget: StateFlow<GroundingTarget?> = _lastFoundTarget.asStateFlow()

    fun setSearchQuery(query: String?) {
        val trimmed = query?.trim()
        _activeSearchQuery.value = if (trimmed.isNullOrBlank()) null else trimmed.lowercase()
        _lastFoundTarget.value = null
        Log.i(TAG, "Active Locate query set to: ${_activeSearchQuery.value}")
    }

    fun clearSearch() {
        _activeSearchQuery.value = null
        _lastFoundTarget.value = null
    }

    /**
     * Checks if a detected object matches the active open-vocabulary search query,
     * and produces fine-grained directional guidance.
     */
    fun matchAndGround(
        detectedLabel: String,
        box: Rect,
        frameWidth: Int,
        frameHeight: Int,
        confidence: Float
    ): GroundingTarget? {
        val currentQuery = _activeSearchQuery.value ?: return null
        val labelLower = detectedLabel.lowercase()

        // Open-vocabulary keyword matching
        val isMatch = labelLower.contains(currentQuery) ||
                currentQuery.contains(labelLower) ||
                matchesSynonyms(currentQuery, labelLower)

        if (!isMatch) return null

        val context = spatialContextEngine.analyze(box, frameWidth, frameHeight)
        val vietnameseName = translate(labelLower)

        val guidance = when (context.direction) {
            SpatialDirection.LEFT -> "Chuyển hướng camera sang trái để hướng vào $vietnameseName."
            SpatialDirection.RIGHT -> "Chuyển hướng camera sang phải để hướng vào $vietnameseName."
            SpatialDirection.CENTER -> {
                if (context.isNear) "Mục tiêu $vietnameseName đang ở ngay trước mặt bạn (rất gần)."
                else "Mục tiêu $vietnameseName đang ở thẳng phía trước."
            }
            else -> "$vietnameseName ở phía trước."
        }

        val target = GroundingTarget(
            query = currentQuery,
            label = labelLower,
            vietnameseName = vietnameseName,
            box = box,
            confidence = confidence,
            spatialDirection = context.direction,
            guidanceInstruction = guidance
        )

        _lastFoundTarget.value = target
        return target
    }

    private fun matchesSynonyms(query: String, label: String): Boolean {
        return when {
            query in listOf("người", "ai", "bạn", "person", "human") && label in listOf("person", "human") -> true
            query in listOf("xe", "ô tô", "oto", "car", "xe máy", "motorcycle", "xe buýt", "bus") && label in listOf("car", "bus", "truck", "motorcycle", "bicycle") -> true
            query in listOf("ghế", "chỗ ngồi", "chair", "seat") && label in listOf("chair", "couch", "seat") -> true
            query in listOf("bàn", "table", "desk") && label in listOf("table", "desk") -> true
            query in listOf("cửa", "door", "lối ra", "exit") && label in listOf("door", "gate") -> true
            query in listOf("bậc thang", "cầu thang", "stairs", "step") && label in listOf("stairs", "steps") -> true
            else -> false
        }
    }

    private fun translate(label: String): String {
        return when {
            label.contains("person") -> "người"
            label.contains("car") -> "xe ô tô"
            label.contains("motorcycle") -> "xe máy"
            label.contains("bicycle") -> "xe đạp"
            label.contains("bus") -> "xe buýt"
            label.contains("chair") -> "ghế"
            label.contains("table") -> "bàn"
            label.contains("door") -> "cửa"
            label.contains("stair") -> "bậc thang"
            else -> label
        }
    }
}
