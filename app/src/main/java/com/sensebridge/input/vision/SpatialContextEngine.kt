package com.sensebridge.input.vision

import android.graphics.Rect
import com.sensebridge.core.model.SpatialDirection

data class SpatialObjectContext(
    val direction: SpatialDirection,
    val isNear: Boolean,
    val isFar: Boolean,
    val normalizedX: Float,
    val areaRatio: Float
)

/**
 * Computes spatial direction (Left/Center/Right) and relative proximity (Near/Far)
 * based on bounding box geometry relative to the overall camera frame.
 */
class SpatialContextEngine {

    companion object {
        private const val LEFT_BOUNDARY_THRESHOLD = 0.35f
        private const val RIGHT_BOUNDARY_THRESHOLD = 0.65f
        private const val NEAR_AREA_RATIO_THRESHOLD = 0.28f
        private const val FAR_AREA_RATIO_THRESHOLD = 0.10f
    }

    /**
     * Determines spatial direction and proximity for a detected object bounding box.
     */
    fun analyze(box: Rect, frameWidth: Int, frameHeight: Int): SpatialObjectContext {
        if (frameWidth <= 0 || frameHeight <= 0) {
            return SpatialObjectContext(
                direction = SpatialDirection.CENTER,
                isNear = false,
                isFar = false,
                normalizedX = 0.5f,
                areaRatio = 0f
            )
        }

        val centerX = (box.left + box.right) / 2.0f
        val normX = (centerX / frameWidth).coerceIn(0f, 1f)

        val direction = when {
            normX < LEFT_BOUNDARY_THRESHOLD -> SpatialDirection.LEFT
            normX > RIGHT_BOUNDARY_THRESHOLD -> SpatialDirection.RIGHT
            else -> SpatialDirection.CENTER
        }

        val boxArea = (box.width().coerceAtLeast(0) * box.height().coerceAtLeast(0)).toFloat()
        val frameArea = (frameWidth * frameHeight).toFloat()
        val areaRatio = if (frameArea > 0) (boxArea / frameArea).coerceIn(0f, 1f) else 0f

        val isNear = areaRatio >= NEAR_AREA_RATIO_THRESHOLD
        val isFar = areaRatio <= FAR_AREA_RATIO_THRESHOLD

        return SpatialObjectContext(
            direction = direction,
            isNear = isNear,
            isFar = isFar,
            normalizedX = normX,
            areaRatio = areaRatio
        )
    }
}
