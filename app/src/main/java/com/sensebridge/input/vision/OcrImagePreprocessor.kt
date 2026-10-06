package com.sensebridge.input.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import androidx.camera.core.ImageProxy

object OcrImagePreprocessor {
    private const val MAX_LONG_EDGE_PX = 2560
    private const val CONTRAST_SCALE = 1.25f
    private const val BRIGHTNESS_OFFSET = 8.0f

    fun toViewportBitmap(imageProxy: ImageProxy, applyContrastBoost: Boolean = false): Bitmap {
        val decoded = imageProxy.toBitmap()
        val cropped = cropToViewport(decoded, imageProxy)
        val downscaled = downscaleIfNeeded(cropped)
        return if (applyContrastBoost) {
            replaceAndRecycle(downscaled) { enhanceContrast(downscaled) }
        } else {
            downscaled
        }
    }

    /**
     * Hardware-accelerated contrast and brightness optimization using ColorMatrix.
     * Enhances faint text on low-contrast backgrounds (faded paper, distant signage).
     */
    fun enhanceContrast(source: Bitmap, contrast: Float = CONTRAST_SCALE, brightness: Float = BRIGHTNESS_OFFSET): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun cropToViewport(source: Bitmap, imageProxy: ImageProxy): Bitmap {
        val isFullSizeFrame = source.width == imageProxy.width && source.height == imageProxy.height
        val crop = Rect(imageProxy.cropRect)
        val coversWholeFrame = crop.width() == source.width && crop.height() == source.height
        if (!isFullSizeFrame || coversWholeFrame || crop.isEmpty) return source
        if (!crop.intersect(0, 0, source.width, source.height)) return source

        return replaceAndRecycle(source) {
            Bitmap.createBitmap(source, crop.left, crop.top, crop.width(), crop.height())
        }
    }

    private fun downscaleIfNeeded(source: Bitmap): Bitmap {
        val longEdge = maxOf(source.width, source.height)
        if (longEdge <= MAX_LONG_EDGE_PX) return source

        val scale = MAX_LONG_EDGE_PX.toFloat() / longEdge
        val targetWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
        return replaceAndRecycle(source) {
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        }
    }

    private inline fun replaceAndRecycle(source: Bitmap, transform: () -> Bitmap): Bitmap {
        val result = transform()
        if (result !== source) source.recycle()
        return result
    }
}
