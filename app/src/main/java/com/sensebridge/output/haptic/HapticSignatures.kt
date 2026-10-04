package com.sensebridge.output.haptic

/**
 * Distinct vibration rhythms per sound type, so a deaf user with the phone in a pocket can tell
 * WHAT happened without looking at the screen.
 *
 * Design rules:
 * - Signatures differ by RHYTHM (count/length of pulses), not amplitude, because cheap ERM motors
 *   cannot render amplitude differences reliably.
 * - Urgent signatures last <= 1 s so the ~1 s continuation repeat doesn't truncate them.
 * - Attention signatures (doorbell, knock) are >= 300 ms total; an 80 ms tick is easily missed
 *   through clothing.
 *
 * Timings follow the Android waveform format: [delay, on, off, on, ...] in milliseconds.
 */
object HapticSignatures {

    /** Labels are the [com.sensebridge.input.sound.SoundGroup] keys plus the loud-impact fallback. */
    val TIMINGS_BY_LABEL: Map<String, LongArray> = mapOf(
        "car_horn" to longArrayOf(0, 150, 100, 150, 100, 150),      // ta-ta-ta (3 short)
        "siren" to longArrayOf(0, 250, 100, 600),                    // short-LONG
        "fire_alarm" to longArrayOf(0, 400, 150, 400),               // LONG-LONG
        "glass_break" to longArrayOf(0, 800),                        // one long hit
        "explosion" to longArrayOf(0, 800),                          // one long hit
        "loud_sound" to longArrayOf(0, 800),                         // one long hit
        "scream" to longArrayOf(0, 100, 80, 100, 80, 100, 80, 100),  // 4 rapid
        "car_alarm" to longArrayOf(0, 200, 200, 200, 200, 200),      // even triple
        "baby_cry" to longArrayOf(0, 300, 200, 300),                 // medium-medium
        "doorbell" to longArrayOf(0, 150, 250, 150),                 // ding ... dong
        "knock" to longArrayOf(0, 70, 130, 70, 130, 70),             // knock-knock-knock
        "phone_ring" to longArrayOf(0, 400, 300, 400)                // ring ... ring
    )

    /** Amplitude used for every "on" segment when the motor supports amplitude control. */
    const val URGENT_AMPLITUDE = 255
    const val NORMAL_AMPLITUDE = 190

    /**
     * Returns the waveform timings for [label], or null when the label has no custom rhythm.
     */
    fun timingsFor(label: String): LongArray? = TIMINGS_BY_LABEL[label]

    /**
     * Builds an amplitude array matching [timings]: 0 for delay/off segments, [onAmplitude] for on.
     */
    fun amplitudesFor(timings: LongArray, onAmplitude: Int): IntArray =
        IntArray(timings.size) { index -> if (index % 2 == 1) onAmplitude else 0 }

    /** Total duration of a waveform in milliseconds. */
    fun durationOf(timings: LongArray): Long = timings.sum()
}
