package com.sensebridge.input.vision

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection

data class VisionDescription(
    val vietnameseTitle: String,
    val spokenSentence: String,
    val priority: PriorityLevel
)

/**
 * Transforms detected object labels and spatial positions into concise Vietnamese sentences.
 */
class SentenceGenerator {

    /**
     * Generates natural language description and priority for a visual observation.
     */
    fun generateDescription(
        rawLabel: String,
        context: SpatialObjectContext
    ): VisionDescription {
        val label = rawLabel.lowercase().trim()
        val vietnameseName = translateLabel(label)
        val directionPhrase = context.direction.vietnameseLabel
        val proximityPhrase = when {
            context.isNear -> "ở gần"
            context.isFar -> "ở xa"
            else -> ""
        }

        // Special handling for high-risk hazards: Stairs, moving vehicles
        val isVehicle = label in listOf("car", "bus", "truck", "motorcycle", "vehicle")
        val isHazard = label in listOf("stairs", "staircase", "steps")

        val priority = when {
            isVehicle && context.isNear -> PriorityLevel.WARNING_P1
            isHazard -> PriorityLevel.WARNING_P1
            context.isNear -> PriorityLevel.ATTENTION_P2
            label in listOf("person", "door") -> PriorityLevel.ATTENTION_P2
            else -> PriorityLevel.INFO_P3
        }

        val sentence = buildString {
            if (priority == PriorityLevel.WARNING_P1) {
                append("Cẩn thận, ")
            }
            append("Có $vietnameseName ")
            if (directionPhrase.isNotBlank()) {
                append("$directionPhrase ")
            }
            if (proximityPhrase.isNotBlank()) {
                append("$proximityPhrase.")
            } else {
                append(".")
            }
        }.trim()

        return VisionDescription(
            vietnameseTitle = vietnameseName.replaceFirstChar { it.uppercase() },
            spokenSentence = sentence,
            priority = priority
        )
    }

    private fun translateLabel(label: String): String {
        return when {
            label.contains("person") || label.contains("human") -> "người"
            label.contains("car") || label.contains("automobile") -> "xe ô tô"
            label.contains("motorcycle") || label.contains("motorbike") -> "xe máy"
            label.contains("bicycle") || label.contains("bike") -> "xe đạp"
            label.contains("bus") -> "xe buýt"
            label.contains("truck") -> "xe tải"
            label.contains("chair") || label.contains("seat") -> "ghế"
            label.contains("table") || label.contains("desk") -> "bàn"
            label.contains("door") -> "cửa"
            label.contains("stair") || label.contains("step") -> "bậc thang"
            label.contains("home good") -> "vật dụng trong nhà"
            label.contains("food") -> "thực phẩm"
            label.contains("fashion good") -> "trang phục"
            label.contains("plant") -> "cây cối"
            label.contains("place") -> "khu vực xung quanh"
            else -> label
        }
    }
}
