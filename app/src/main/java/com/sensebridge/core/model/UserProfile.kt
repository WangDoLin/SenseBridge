package com.sensebridge.core.model

/**
 * User and AI Assistant identity profile for personalized conversational address.
 * Allows users to define custom names and Vietnamese relational pronouns.
 */
data class UserProfile(
    val userName: String = "bạn",
    val userPronoun: String = "bạn",
    val aiName: String = "SenseBridge",
    val aiPronoun: String = "mình",
    val isOnboardingCompleted: Boolean = false
) {
    /**
     * Capitalized pronouns for beginning sentences.
     */
    val capitalizedUserPronoun: String
        get() = userPronoun.trim().replaceFirstChar { it.uppercase() }

    val capitalizedAiPronoun: String
        get() = aiPronoun.trim().replaceFirstChar { it.uppercase() }

    /**
     * Generates a warm, natural personalized initial greeting.
     */
    fun buildGreeting(): String {
        val cleanName = userName.trim()
        val isDefault = cleanName.equals("bạn", ignoreCase = true) && aiName.equals("SenseBridge", ignoreCase = true)
        return if (isDefault) {
            "Xin chào bạn! Mình là AI SenseBridge."
        } else {
            val displayName = if (cleanName.isNotBlank() && !cleanName.equals(userPronoun, ignoreCase = true)) {
                "$userPronoun $cleanName"
            } else {
                userPronoun
            }
            "Xin chào $displayName! $capitalizedAiPronoun là $aiName, rất vui được đồng hành và hỗ trợ $userPronoun."
        }
    }

    /**
     * Generates natural speech acknowledging assistance.
     */
    fun buildGratitudeReply(): String {
        val cleanName = userName.trim()
        val displayName = if (cleanName.isNotBlank() && !cleanName.equals(userPronoun, ignoreCase = true) && !cleanName.equals("bạn", ignoreCase = true)) {
            "$userPronoun $cleanName"
        } else {
            userPronoun
        }
        return "Rất vui được hỗ trợ $displayName! Cần quan sát thêm gì cứ bảo $aiName nhé."
    }

    /**
     * Generates self-introduction speech.
     */
    fun buildIdentityReply(): String {
        val isDefault = aiName.equals("SenseBridge", ignoreCase = true)
        val nameLabel = if (isDefault) "SenseAI" else aiName
        val cleanName = userName.trim()
        val displayName = if (cleanName.isNotBlank() && !cleanName.equals(userPronoun, ignoreCase = true) && !cleanName.equals("bạn", ignoreCase = true)) {
            "$userPronoun $cleanName"
        } else {
            userPronoun
        }
        return "$capitalizedAiPronoun là $nameLabel, trợ lý giác quan riêng của $displayName trong SenseBridge."
    }

    /**
     * Replaces conversational placeholders with user-configured names and pronouns.
     */
    fun formatText(template: String): String {
        return template
            .replace("{USER}", if (userName.isNotBlank()) "$userPronoun $userName".trim() else userPronoun)
            .replace("{USER_NAME}", userName.trim())
            .replace("{USER_PRONOUN}", userPronoun.trim())
            .replace("{CAP_USER_PRONOUN}", capitalizedUserPronoun)
            .replace("{AI_NAME}", aiName.trim())
            .replace("{AI_PRONOUN}", aiPronoun.trim())
            .replace("{CAP_AI_PRONOUN}", capitalizedAiPronoun)
    }

    companion object {
        val DEFAULT = UserProfile()
    }
}
