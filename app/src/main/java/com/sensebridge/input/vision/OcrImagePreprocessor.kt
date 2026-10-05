package com.sensebridge.input.vision

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.core.ImageProxy

object OcrImagePreprocessor {
    private const val MAX_LONG_EDGE_PX = 2560

    fun toViewportBitmap(imageProxy: ImageProxy): Bitmap {
        val decoded = imageProxy.toBitmap()
        val cropped = cropToViewport(decoded, imageProxy)
        return downscaleIfNeeded(cropped)
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
