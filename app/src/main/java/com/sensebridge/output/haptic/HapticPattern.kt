package com.sensebridge.output.haptic

/**
 * Predefined haptic timings and amplitudes for different emergency levels.
 * Distinguishes between LRA (high definition) and ERM (legacy motor) gracefully.
 */
object HapticPattern {
    // P0: Critical Danger (e.g., Fire alarm, horn right next to user)
    val P0_TIMINGS = longArrayOf(0, 300, 100, 300, 100, 500)
    val P0_AMPLITUDES = intArrayOf(0, 255, 0, 255, 0, 255)

    // P1: Warning (e.g., Approaching car, distant siren, baby crying)
    val P1_TIMINGS = longArrayOf(0, 200, 150, 200)
    val P1_AMPLITUDES = intArrayOf(0, 200, 0, 200)

    // P2: Attention / Notification (e.g., Doorbell, text detected)
    const val P2_DURATION_MS = 80L
    const val P2_AMPLITUDE = 160

    // P3: Gentle Info tick
    const val P3_DURATION_MS = 40L
    const val P3_AMPLITUDE = 100
}
