package com.sensebridge.input.vision

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Throttles camera image analysis frames to a battery-friendly frame rate (8-10 FPS)
 * and drops incoming frames if previous AI inference is still computing.
 *
 * @param targetFps Desired maximum analysis frames per second (default: 10 FPS).
 */
class FrameRateThrottle(
    targetFps: Int = 10
) {
    private val minIntervalMs: Long = (1000.0 / targetFps.coerceIn(1, 30)).toLong()
    private var lastProcessedTimestamp: Long = 0L
    private val isBusy = AtomicBoolean(false)

    /**
     * Determines whether the current frame should be analyzed.
     */
    fun shouldProcessFrame(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastProcessedTimestamp < minIntervalMs) {
            return false
        }

        // Try to acquire processing lock; if already busy, drop this frame
        if (isBusy.compareAndSet(false, true)) {
            lastProcessedTimestamp = now
            return true
        }

        return false
    }

    /**
     * Must be called when frame inference concludes to release the lock.
     */
    fun onFrameCompleted() {
        isBusy.set(false)
    }

    /**
     * Resets throttle state.
     */
    fun reset() {
        lastProcessedTimestamp = 0L
        isBusy.set(false)
    }
}
