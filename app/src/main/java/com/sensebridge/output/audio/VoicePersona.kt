package com.sensebridge.output.audio

/**
 * Acoustic personas and voice profiles for Text-To-Speech synthesis.
 * Configures vocal pitch (F0 frequency modulation), speech rate, and acoustic timbre.
 */
enum class VoicePersona(
    val id: String,
    val displayNameVi: String,
    val descriptionVi: String,
    val pitch: Float,
    val speechRate: Float
) {
    DEFAULT(
        id = "DEFAULT",
        displayNameVi = "Mặc định hệ thống",
        descriptionVi = "Giọng đọc tiêu chuẩn từ bộ máy TTS của hệ điều hành",
        pitch = 1.0f,
        speechRate = 1.0f
    ),
    NATURAL_FEMALE(
        id = "NATURAL_FEMALE",
        displayNameVi = "Nữ nhẹ nhàng",
        descriptionVi = "Tông F0 nâng cao, âm sắc trong trẻo và dễ chịu",
        pitch = 1.18f,
        speechRate = 1.0f
    ),
    WARM_MALE(
        id = "WARM_MALE",
        displayNameVi = "Nam trầm ấm",
        descriptionVi = "Tần số F0 trầm sâu, giọng đọc điềm đạm và đĩnh đạc",
        pitch = 0.82f,
        speechRate = 0.95f
    ),
    ENERGETIC_ASSISTANT(
        id = "ENERGETIC_ASSISTANT",
        displayNameVi = "Trợ lý năng động",
        descriptionVi = "Nhịp điệu dứt khoát, âm vực cao, tối ưu cho phản hồi nhanh",
        pitch = 1.28f,
        speechRate = 1.15f
    ),
    CALM_NARRATOR(
        id = "CALM_NARRATOR",
        displayNameVi = "Thong thả truyền cảm",
        descriptionVi = "Tốc độ vừa phải, thích hợp nghe đọc văn bản và sách nói dài",
        pitch = 0.95f,
        speechRate = 0.85f
    );

    companion object {
        const val SAMPLE_PHRASE = "Xin chào, tôi là trợ lý SenseBridge luôn đồng hành cùng bạn."

        fun fromId(id: String): VoicePersona {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
