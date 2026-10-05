package com.sensebridge.input.sound

import com.sensebridge.core.model.PriorityLevel

/**
 * Emergency sound groups mapped to EXACT YAMNet label names (verified against
 * the yamnet_label_list.txt embedded in assets/yamnet.tflite).
 *
 * Exact matching avoids false positives from substring matching, e.g.
 * "French horn" (instrument) or "Engine knocking" (motor) being mistaken for
 * a car horn or a door knock.
 *
 * Thresholds are initial hypotheses and should be tuned with on-device data.
 */
enum class SoundGroup(
    val key: String,
    val yamnetLabels: Set<String>,
    val threshold: Float,
    val priority: PriorityLevel,
    val vietnameseTitle: String,
    val spokenText: String
) {
    CAR_HORN(
        "car_horn",
        setOf("Vehicle horn, car horn, honking", "Toot", "Air horn, truck horn"),
        0.30f, PriorityLevel.CRITICAL_P0,
        "Còi xe", "Cảnh báo, có tiếng còi xe!"
    ),
    SIREN(
        "siren",
        setOf(
            "Siren", "Civil defense siren", "Police car (siren)",
            "Ambulance (siren)", "Fire engine, fire truck (siren)", "Emergency vehicle"
        ),
        0.30f, PriorityLevel.CRITICAL_P0,
        "Còi xe ưu tiên", "Cảnh báo, có còi xe ưu tiên đến gần!"
    ),
    FIRE_ALARM(
        "fire_alarm",
        setOf("Smoke detector, smoke alarm", "Fire alarm"),
        0.30f, PriorityLevel.CRITICAL_P0,
        "Chuông báo cháy", "Nguy hiểm, chuông báo cháy đang reo!"
    ),
    GLASS_BREAK(
        "glass_break",
        // "Glass" is excluded on purpose: in AudioSet it also covers clinking dishes/glasses
        setOf("Shatter", "Smash, crash", "Breaking"),
        0.40f, PriorityLevel.CRITICAL_P0,
        "Tiếng vỡ / va chạm", "Cảnh báo, có tiếng đồ vỡ hoặc va chạm mạnh!"
    ),
    EXPLOSION(
        "explosion",
        setOf("Explosion", "Gunshot, gunfire", "Boom"),
        0.40f, PriorityLevel.CRITICAL_P0,
        "Tiếng nổ", "Nguy hiểm, có tiếng nổ lớn!"
    ),
    SCREAM(
        "scream",
        setOf("Screaming"),
        0.40f, PriorityLevel.CRITICAL_P0,
        "Tiếng la hét", "Cảnh báo, có người đang la hét!"
    ),
    CAR_ALARM(
        "car_alarm",
        setOf("Car alarm"),
        0.35f, PriorityLevel.WARNING_P1,
        "Báo động xe", "Có tiếng báo động xe."
    ),
    BABY_CRY(
        "baby_cry",
        setOf("Baby cry, infant cry"),
        0.35f, PriorityLevel.WARNING_P1,
        "Em bé khóc", "Có tiếng em bé đang khóc."
    ),
    DOORBELL(
        "doorbell",
        setOf("Doorbell", "Ding-dong"),
        0.35f, PriorityLevel.ATTENTION_P2,
        "Chuông cửa", "Có tiếng chuông cửa."
    ),
    KNOCK(
        "knock",
        setOf("Knock"),
        0.40f, PriorityLevel.ATTENTION_P2,
        "Gõ cửa", "Có người gõ cửa."
    ),
    PHONE_RING(
        "phone_ring",
        setOf("Telephone bell ringing", "Ringtone", "Alarm clock"),
        0.40f, PriorityLevel.ATTENTION_P2,
        "Chuông điện thoại", "Có chuông điện thoại hoặc báo thức."
    ),
    DOG_BARK(
        "dog_bark",
        setOf("Bark", "Bow-wow"),
        0.40f, PriorityLevel.INFO_P3,
        "Chó sủa", "Có tiếng chó sủa gần đây."
    );

    companion object {
        /** Score above which a single window is trusted without temporal confirmation. */
        const val STRONG_SCORE_MARGIN = 0.25f

        private val labelToGroup: Map<String, SoundGroup> =
            entries.flatMap { group -> group.yamnetLabels.map { it to group } }.toMap()

        /** Returns the group owning [yamnetLabel], or null for non-emergency sounds (fan, speech…). */
        fun forLabel(yamnetLabel: String): SoundGroup? = labelToGroup[yamnetLabel.trim()]

        fun forKey(key: String): SoundGroup? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Pure scoring helpers kept free of Android dependencies so they can be unit tested.
 */
object AudioLabelTaxonomy {

    /**
     * Aggregates multi-label YAMNet scores into per-group scores (max over member labels).
     * Labels not belonging to any group (Mechanical fan, Speech, Music…) are ignored, which is
     * what makes everyday noise unable to raise an alarm on its own.
     */
    fun scoreGroups(labelScores: List<Pair<String, Float>>): Map<SoundGroup, Float> {
        val result = mutableMapOf<SoundGroup, Float>()
        for ((label, score) in labelScores) {
            val group = SoundGroup.forLabel(label) ?: continue
            if (score > (result[group] ?: 0f)) result[group] = score
        }
        return result
    }

    /** Groups whose score passes their own threshold in this window. */
    fun passingGroups(groupScores: Map<SoundGroup, Float>): Map<SoundGroup, Float> =
        groupScores.filter { (group, score) -> score >= group.threshold }

    /** Picks the most urgent group (lowest priority weight), breaking ties by score. */
    fun mostUrgent(groups: Map<SoundGroup, Float>): SoundGroup? =
        groups.entries
            .sortedWith(compareBy<Map.Entry<SoundGroup, Float>> { it.key.priority.weight }.thenByDescending { it.value })
            .firstOrNull()?.key

    /** Builds the UI/TTS result for a confirmed [group]. */
    fun toResult(group: SoundGroup, confidence: Float): AudioClassificationResult =
        AudioClassificationResult(
            label = group.key,
            vietnameseTitle = group.vietnameseTitle,
            spokenText = group.spokenText,
            confidence = confidence,
            priority = group.priority
        )

    /** Exact-label mapping of a single YAMNet label; null for any non-emergency sound. */
    fun mapLabel(rawLabel: String, confidence: Float): AudioClassificationResult? =
        SoundGroup.forLabel(rawLabel)?.let { toResult(it, confidence) }
}
