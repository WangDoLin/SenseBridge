package com.sensebridge.core.ai

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import com.sensebridge.input.vision.ScannedDomain
import com.sensebridge.input.vision.SmartDocumentParser

data class AiProactiveInsight(
    val title: String,
    val spokenText: String,
    val priority: PriorityLevel,
    val insightType: String
)

data class AiQueryAnswer(
    val answerText: String,
    val priority: PriorityLevel
)

class AiMultimodalReasoner {

    companion object {
        private const val VEHICLE_LABELS = "car,bus,truck,motorcycle,motorbike,vehicle"
        private const val HORN_KEYWORDS = "horn,beep"
        private const val SIREN_KEYWORDS = "siren,ambulance,police,fire truck,alarm"
        private const val HAZARD_LABELS = "stairs,staircase,steps,hole"
        private const val DISTRESS_KEYWORDS = "screaming,glass,crying,yell"
        private const val IMPORTANT_SIGNS = "cấp cứu,lối thoát,cấm,nguy hiểm,bệnh viện,nhà thuốc,wc,vệ sinh"
    }

    fun evaluateSceneThreat(snapshot: SceneSnapshot): AiProactiveInsight? {
        val hasVehicle = snapshot.objects.any { isVehicle(it.label) }
        val nearVehicle = snapshot.objects.firstOrNull { isVehicle(it.label) && it.isNear }
        val hasHorn = snapshot.sounds.any { it.label.containsAny(HORN_KEYWORDS) }
        val sirenSound = snapshot.sounds.firstOrNull { it.label.containsAny(SIREN_KEYWORDS) }

        if (nearVehicle != null && hasHorn) {
            val direction = nearVehicle.direction.vietnameseLabel
            return AiProactiveInsight(
                title = "Nguy hiểm: Xe bấm còi gần",
                spokenText = "Cảnh báo khẩn cấp, xe $direction đang bấm còi và ở rất gần bạn!",
                priority = PriorityLevel.CRITICAL_P0,
                insightType = "APPROACHING_VEHICLE_HORN"
            )
        }

        if (sirenSound != null) {
            return AiProactiveInsight(
                title = "Có còi ưu tiên",
                spokenText = "Chú ý, phát hiện tiếng còi cứu thương hoặc xe ưu tiên đang đến gần.",
                priority = PriorityLevel.CRITICAL_P0,
                insightType = "EMERGENCY_SIREN"
            )
        }

        if (hasVehicle && hasHorn) {
            return AiProactiveInsight(
                title = "Cảnh báo còi xe",
                spokenText = "Có xe đang bấm còi cảnh báo phía trước.",
                priority = PriorityLevel.WARNING_P1,
                insightType = "VEHICLE_HORN"
            )
        }

        val hazardObject = snapshot.objects.firstOrNull { it.label.containsAny(HAZARD_LABELS) }
        if (hazardObject != null) {
            val dir = hazardObject.direction.vietnameseLabel
            return AiProactiveInsight(
                title = "Cảnh báo chướng ngại vật",
                spokenText = "Cẩn thận, có ${hazardObject.vietnameseLabel} ở $dir.",
                priority = PriorityLevel.WARNING_P1,
                insightType = "STAIRS_HAZARD"
            )
        }

        val distressSound = snapshot.sounds.firstOrNull { it.label.containsAny(DISTRESS_KEYWORDS) }
        if (distressSound != null) {
            return AiProactiveInsight(
                title = "Âm thanh bất thường",
                spokenText = "Phát hiện ${distressSound.vietnameseLabel}, vui lòng chú ý xung quanh.",
                priority = PriorityLevel.WARNING_P1,
                insightType = "DISTRESS_SOUND"
            )
        }

        val text = snapshot.latestText
        if (text != null && text.containsAny(IMPORTANT_SIGNS)) {
            return AiProactiveInsight(
                title = "Biển báo quan trọng",
                spokenText = "Biển báo quan sát thấy: $text.",
                priority = PriorityLevel.ATTENTION_P2,
                insightType = "SIGNAGE"
            )
        }

        return null
    }

    fun answerUserQuery(rawQuery: String, snapshot: SceneSnapshot): AiQueryAnswer {
        val query = rawQuery.lowercase().trim()

        return when {
            query.containsAny("an toàn,qua đường,đi được,băng qua") -> {
                buildSafetyAnswer(snapshot)
            }
            query.containsAny("xung quanh,trước mặt,phía trước,quan sát,thấy gì,có gì") -> {
                buildSurroundingsAnswer(snapshot)
            }
            query.containsAny("chữ,biển,bảng,đọc,viết") -> {
                buildReadTextAnswer(snapshot)
            }
            query.containsAny("tiếng gì,âm thanh,nghe thấy,tiếng động,ồn") -> {
                buildSoundAnswer(snapshot)
            }
            else -> {
                buildGeneralSummaryAnswer(snapshot)
            }
        }
    }

    private fun buildSafetyAnswer(snapshot: SceneSnapshot): AiQueryAnswer {
        val nearVehicle = snapshot.objects.firstOrNull { isVehicle(it.label) && it.isNear }
        val hasHorn = snapshot.sounds.any { it.label.containsAny(HORN_KEYWORDS) }
        val siren = snapshot.sounds.any { it.label.containsAny(SIREN_KEYWORDS) }
        val anyVehicle = snapshot.objects.firstOrNull { isVehicle(it.label) }

        if (nearVehicle != null || hasHorn || siren) {
            val reason = when {
                nearVehicle != null -> "có ${nearVehicle.vietnameseLabel} ở cự ly gần ${nearVehicle.direction.vietnameseLabel}"
                hasHorn -> "đang có tiếng còi xe bấm liên tục"
                else -> "có tiếng còi xe ưu tiên đang di chuyển"
            }
            return AiQueryAnswer(
                answerText = "Chưa an toàn để di chuyển: $reason. Vui lòng dừng lại và quan sát kỹ.",
                priority = PriorityLevel.WARNING_P1
            )
        }

        if (anyVehicle != null) {
            return AiQueryAnswer(
                answerText = "Cần cẩn thận: Có ${anyVehicle.vietnameseLabel} ở ${anyVehicle.direction.vietnameseLabel}, hãy chú ý trước khi bước tiếp.",
                priority = PriorityLevel.ATTENTION_P2
            )
        }

        return AiQueryAnswer(
            answerText = "Khu vực phía trước tương đối an toàn, không phát hiện phương tiện hoặc âm thanh nguy hiểm.",
            priority = PriorityLevel.INFO_P3
        )
    }

    private fun buildSurroundingsAnswer(snapshot: SceneSnapshot): AiQueryAnswer {
        val objectDescriptions = if (snapshot.objects.isNotEmpty()) {
            val topObjects = snapshot.objects.take(3)
            val parts = topObjects.map {
                val proximity = if (it.isNear) "ở gần" else ""
                "${it.vietnameseLabel} ${it.direction.vietnameseLabel} $proximity".trim()
            }
            "Phát hiện " + parts.joinToString(", ")
        } else {
            "Không thấy vật cản rõ ràng phía trước"
        }

        val soundDescription = if (snapshot.sounds.isNotEmpty()) {
            val topSound = snapshot.sounds.first()
            ", vừa có ${topSound.vietnameseLabel}"
        } else if (snapshot.isNoisy) {
            ", môi trường xung quanh khá ồn (${snapshot.ambientDecibels.toInt()} dB)"
        } else {
            ", không gian xung quanh khá yên tĩnh"
        }

        val textAddition = if (!snapshot.latestText.isNullOrBlank()) {
            ". Biển báo đọc được: ${snapshot.latestText}"
        } else {
            ""
        }

        return AiQueryAnswer(
            answerText = "$objectDescriptions$soundDescription$textAddition.",
            priority = PriorityLevel.ATTENTION_P2
        )
    }

    private fun buildReadTextAnswer(snapshot: SceneSnapshot): AiQueryAnswer {
        val text = snapshot.latestText
        return if (!text.isNullOrBlank()) {
            val parsed = SmartDocumentParser.parse(text)
            val answer = if (parsed.domain != ScannedDomain.GENERAL_TEXT) {
                parsed.speechSummary
            } else {
                "Văn bản đọc được qua camera là: $text."
            }
            AiQueryAnswer(
                answerText = answer,
                priority = PriorityLevel.ATTENTION_P2
            )
        } else {
            AiQueryAnswer(
                answerText = "Hiện tại camera chưa nhận diện được chữ hoặc biển báo nào rõ ràng.",
                priority = PriorityLevel.INFO_P3
            )
        }
    }

    private fun buildSoundAnswer(snapshot: SceneSnapshot): AiQueryAnswer {
        if (snapshot.sounds.isNotEmpty()) {
            val latest = snapshot.sounds.take(2).joinToString(" và ") { it.vietnameseLabel }
            val db = snapshot.ambientDecibels.toInt()
            return AiQueryAnswer(
                answerText = "Vừa phát hiện âm thanh $latest. Cường độ âm thanh khoảng $db đề-xi-ben.",
                priority = PriorityLevel.ATTENTION_P2
            )
        }

        val state = if (snapshot.isNoisy) "tiếng ồn nền khá lớn (${snapshot.ambientDecibels.toInt()} dB)" else "yên tĩnh"
        return AiQueryAnswer(
            answerText = "Không có âm thanh đặc biệt nào, môi trường hiện đang $state.",
            priority = PriorityLevel.INFO_P3
        )
    }

    private fun buildGeneralSummaryAnswer(snapshot: SceneSnapshot): AiQueryAnswer {
        val objectCount = snapshot.objects.size
        val soundCount = snapshot.sounds.size
        val db = snapshot.ambientDecibels.toInt()
        return AiQueryAnswer(
            answerText = "AI đang quan sát: có $objectCount đối tượng trong tầm nhìn, $soundCount sự kiện âm thanh gần đây, mức ồn $db đề-xi-ben.",
            priority = PriorityLevel.INFO_P3
        )
    }

    private fun isVehicle(label: String): Boolean = label.containsAny(VEHICLE_LABELS)

    private fun String.containsAny(csv: String): Boolean {
        val targets = csv.split(",")
        return targets.any { this.contains(it.trim(), ignoreCase = true) }
    }
}
