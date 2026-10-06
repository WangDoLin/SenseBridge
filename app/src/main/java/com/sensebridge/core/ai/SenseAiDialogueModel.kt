package com.sensebridge.core.ai

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.UserProfile
import javax.inject.Inject

enum class SituationState(val vietnameseName: String) {
    EMERGENCY_HAZARD("Tình huống nguy hiểm khẩn cấp"),
    STREET_TRAFFIC("Tình huống giao thông đường phố"),
    INDOOR_NAVIGATION("Tình huống di chuyển trong nhà"),
    SOCIAL_MEETING("Tình huống giao tiếp xã hội"),
    READING_SIGNAGE("Tình huống đọc biển báo và chữ viết"),
    SAFE_QUIET("Môi trường an toàn và yên tĩnh")
}

enum class DialogueIntent {
    GREETING,
    SITUATION_INQUIRY,
    SAFETY_CHECK,
    SURROUNDINGS_OBSERVE,
    OBJECT_IDENTIFICATION,
    READ_TEXT,
    SOUND_INQUIRY,
    HELP_REQUEST,
    FOLLOW_UP,
    SMALLTALK,
    GENERAL
}

data class SituationClassificationResult(
    val state: SituationState,
    val confidence: Float,
    val summary: String,
    val priority: PriorityLevel
)

data class DialogueResponse(
    val replyText: String,
    val intent: DialogueIntent,
    val situation: SituationState,
    val priority: PriorityLevel
)

data class DialogueTurn(
    val userInput: String,
    val aiResponse: String,
    val intent: DialogueIntent,
    val timestamp: Long = System.currentTimeMillis()
)

class SenseAiDialogueModel @Inject constructor(
    private val neuralClassifier: SenseAiNeuralClassifier? = null,
    private val slmEngine: OnDeviceSlmInferenceEngine? = null,
    private val continualLearningEngine: ContinualLearningEngine? = null,
    private val semanticMatcher: SemanticDialogueMatcher = SemanticDialogueMatcher(),
    private val personaNlgEngine: PersonaAwareDialogueGenerator = PersonaAwareDialogueGenerator(),
    private val maxHistoryTurns: Int = 5
) {
    companion object {
        private const val VEHICLE_KEYWORDS = "car,bus,truck,motorcycle,motorbike,vehicle,xe"
        private const val HORN_KEYWORDS = "horn,beep,còi"
        private const val SIREN_KEYWORDS = "siren,ambulance,police,fire truck,báo cháy,cứu thương"
        private const val INDOOR_KEYWORDS = "chair,table,desk,door,couch,sofa,bed,bàn,ghế,cửa"
        private const val HAZARD_KEYWORDS = "stairs,staircase,steps,hole,bậc thang,hố"
        private const val HUMAN_KEYWORDS = "person,human,người"
        private const val SPEECH_KEYWORDS = "speech,talking,conversation,tiếng nói"
        private const val DISTRESS_KEYWORDS = "screaming,glass,crying,yell,la hét,kính vỡ,khóc"
    }

    private val conversationHistory = mutableListOf<DialogueTurn>()
    var userProfile: UserProfile = UserProfile.DEFAULT

    fun classifySituation(snapshot: SceneSnapshot): SituationClassificationResult {
        val hasSiren = snapshot.sounds.any { it.label.containsAny(SIREN_KEYWORDS) }
        val hasDistress = snapshot.sounds.any { it.label.containsAny(DISTRESS_KEYWORDS) }
        val hasNearHazard = snapshot.objects.any { it.label.containsAny(HAZARD_KEYWORDS) && it.isNear }

        if (hasSiren || hasDistress || hasNearHazard) {
            val detail = when {
                hasSiren -> "phát hiện tiếng còi ưu tiên khẩn cấp"
                hasDistress -> "phát hiện âm thanh báo động bất thường"
                else -> "có bậc thang hoặc chướng ngại vật ngay trước mặt"
            }
            return SituationClassificationResult(
                state = SituationState.EMERGENCY_HAZARD,
                confidence = 0.95f,
                summary = "Khu vực đang có tình huống nguy hiểm: $detail.",
                priority = PriorityLevel.CRITICAL_P0
            )
        }

        val hasVehicle = snapshot.objects.any { it.label.containsAny(VEHICLE_KEYWORDS) }
        val hasHorn = snapshot.sounds.any { it.label.containsAny(HORN_KEYWORDS) }
        if (hasVehicle || hasHorn) {
            val detail = when {
                hasVehicle && hasHorn -> "có phương tiện giao thông đang bấm còi"
                hasVehicle -> "có phương tiện giao thông đang ở gần"
                else -> "có tiếng còi xe trong khu vực"
            }
            return SituationClassificationResult(
                state = SituationState.STREET_TRAFFIC,
                confidence = 0.90f,
                summary = "Bạn đang ở trong môi trường giao thông: $detail.",
                priority = PriorityLevel.WARNING_P1
            )
        }

        if (!snapshot.latestText.isNullOrBlank()) {
            return SituationClassificationResult(
                state = SituationState.READING_SIGNAGE,
                confidence = 0.88f,
                summary = "Camera đang nhận diện được văn bản: ${snapshot.latestText}.",
                priority = PriorityLevel.ATTENTION_P2
            )
        }

        val hasPerson = snapshot.objects.any { it.label.containsAny(HUMAN_KEYWORDS) }
        val hasSpeech = snapshot.sounds.any { it.label.containsAny(SPEECH_KEYWORDS) }
        if (hasPerson && (hasSpeech || snapshot.objects.any { it.isNear })) {
            return SituationClassificationResult(
                state = SituationState.SOCIAL_MEETING,
                confidence = 0.85f,
                summary = "Có người đang ở cự ly gần trong không gian giao tiếp.",
                priority = PriorityLevel.ATTENTION_P2
            )
        }

        val hasFurniture = snapshot.objects.any { it.label.containsAny(INDOOR_KEYWORDS) }
        if (hasFurniture) {
            val firstItem = snapshot.objects.first { it.label.containsAny(INDOOR_KEYWORDS) }
            return SituationClassificationResult(
                state = SituationState.INDOOR_NAVIGATION,
                confidence = 0.82f,
                summary = "Không gian trong nhà, phát hiện ${firstItem.vietnameseLabel} ${firstItem.direction.vietnameseLabel}.",
                priority = PriorityLevel.INFO_P3
            )
        }

        return SituationClassificationResult(
            state = SituationState.SAFE_QUIET,
            confidence = 0.80f,
            summary = "Không gian xung quanh yên tĩnh và an toàn, không có chướng ngại vật nổi bật.",
            priority = PriorityLevel.INFO_P3
        )
    }

    fun converse(rawInput: String, snapshot: SceneSnapshot): DialogueResponse {
        val input = rawInput.trim()
        val situationResult = classifySituation(snapshot)
        val intent = detectIntent(input)

        val replyText = generateReply(intent, input, situationResult, snapshot)

        recordTurn(input, replyText, intent)

        return DialogueResponse(
            replyText = replyText,
            intent = intent,
            situation = situationResult.state,
            priority = determineResponsePriority(intent, situationResult)
        )
    }

    suspend fun converseAsync(rawInput: String, snapshot: SceneSnapshot): DialogueResponse {
        val input = rawInput.trim()
        val situationResult = classifySituation(snapshot)
        val intent = detectIntentAsync(input)

        val memoryContext = continualLearningEngine?.retrieveEpisodicMemory(input, snapshot)

        val slmReply = if (slmEngine != null && slmEngine.isReady()) {
            slmEngine.generateResponse(input, snapshot, userProfile)
        } else {
            null
        }

        val baseReply = slmReply ?: generateReply(intent, input, situationResult, snapshot)
        val finalReply = if (!memoryContext.isNullOrBlank() && !baseReply.contains(memoryContext)) {
            "$memoryContext $baseReply"
        } else {
            baseReply
        }

        recordTurn(input, finalReply, intent)

        continualLearningEngine?.recordAndLearnSession(
            userInput = input,
            aiResponse = finalReply,
            intent = intent,
            situation = situationResult.state,
            snapshot = snapshot
        )

        return DialogueResponse(
            replyText = finalReply,
            intent = intent,
            situation = situationResult.state,
            priority = determineResponsePriority(intent, situationResult)
        )
    }

    suspend fun detectIntentAsync(input: String): DialogueIntent {
        if (continualLearningEngine != null) {
            val learned = continualLearningEngine.predictLearnedIntent(input)
            if (learned != null && learned.confidence >= 0.50f) {
                return learned.intent
            }
        }
        return detectIntent(input)
    }

    fun resetDialogue() {
        conversationHistory.clear()
    }

    fun getHistory(): List<DialogueTurn> = conversationHistory.toList()

    fun detectIntent(input: String): DialogueIntent {
        if (neuralClassifier != null && neuralClassifier.isReady()) {
            val prediction = neuralClassifier.classify(input)
            if (prediction.confidence >= 0.40f && prediction.intent != DialogueIntent.GENERAL) {
                return prediction.intent
            }
        }
        val semanticMatch = semanticMatcher.matchSubIntent(input)
        if (semanticMatch.confidence >= 0.30f) {
            when (semanticMatch.act) {
                DialogueAct.OBJECT_INQUIRY -> return DialogueIntent.OBJECT_IDENTIFICATION
                DialogueAct.IDENTITY_INQUIRY,
                DialogueAct.GRATITUDE,
                DialogueAct.FAREWELL,
                DialogueAct.EMPATHY_SUPPORT,
                DialogueAct.WELLBEING_INQUIRY -> return DialogueIntent.SMALLTALK
                else -> {}
            }
        }
        return detectRuleIntent(input)
    }

    private fun detectRuleIntent(input: String): DialogueIntent {
        val text = input.lowercase()
        return when {
            text.containsAny("chào,hello,hi,xin chào,alo") -> DialogueIntent.GREETING
            text.containsAny("đây là cái gì,cái gì đây,đây là gì,vật này là gì,vật gì đây,đồ gì đây,con gì đây,xe gì đây,trước mặt là cái gì,nhìn xem đây là gì,nhìn xem có gì") -> DialogueIntent.OBJECT_IDENTIFICATION
            text.containsAny("đọc,chữ,biển,bảng,viết gì") -> DialogueIntent.READ_TEXT
            text.containsAny("cứu,cấp cứu,nguy hiểm quá,giúp tôi với,giúp với,cứu tôi") -> DialogueIntent.HELP_REQUEST
            text.containsAny("an toàn,qua đường,băng qua,đi được,đi tiếp,nguy hiểm không,bước tiếp") -> DialogueIntent.SAFETY_CHECK
            text.containsAny("tình hình,tình huống,đang ở đâu,ở đâu đây,thế nào rồi") -> DialogueIntent.SITUATION_INQUIRY
            text.containsAny("xung quanh,trước mặt,phía trước,quan sát,thấy gì,có gì") -> DialogueIntent.SURROUNDINGS_OBSERVE
            text.containsAny("tiếng gì,âm thanh,nghe gì,tiếng động,ồn") -> DialogueIntent.SOUND_INQUIRY
            text.containsAny("còn bây giờ,thế bây giờ,hiện tại thì sao,lúc này") -> DialogueIntent.FOLLOW_UP
            text.containsAny("cảm ơn,thanks,bạn là ai,tên gì,tên là gì,tên bạn,bạn tên,tạm biệt,bye") -> DialogueIntent.SMALLTALK
            text.contains("giúp") -> DialogueIntent.HELP_REQUEST
            else -> DialogueIntent.GENERAL
        }
    }

    private fun generateReply(
        intent: DialogueIntent,
        rawInput: String,
        situation: SituationClassificationResult,
        snapshot: SceneSnapshot
    ): String {
        return when (intent) {
            DialogueIntent.GREETING -> {
                val greeting = userProfile.buildGreeting()
                "$greeting Hiện tại ${situation.summary} ${userProfile.capitalizedAiPronoun} luôn sẵn sàng quan sát và hỗ trợ ${userProfile.userPronoun}."
            }
            DialogueIntent.SITUATION_INQUIRY -> {
                "Báo cáo tình huống: ${situation.summary} Mức ồn môi trường hiện tại là ${snapshot.ambientDecibels.toInt()} đề-xi-ben."
            }
            DialogueIntent.SAFETY_CHECK -> {
                generateSafetyReply(situation, snapshot)
            }
            DialogueIntent.SURROUNDINGS_OBSERVE -> {
                generateSurroundingsReply(snapshot)
            }
            DialogueIntent.OBJECT_IDENTIFICATION -> {
                generateObjectIdentificationReply(snapshot)
            }
            DialogueIntent.READ_TEXT -> {
                if (!snapshot.latestText.isNullOrBlank()) {
                    "Camera đọc được dòng chữ: \"${snapshot.latestText}\"."
                } else {
                    "Hiện tại camera chưa quét thấy chữ viết hoặc biển báo nào rõ ràng."
                }
            }
            DialogueIntent.SOUND_INQUIRY -> {
                generateSoundReply(snapshot)
            }
            DialogueIntent.HELP_REQUEST -> {
                "Đừng lo lắng, ${userProfile.aiPronoun} đang kích hoạt chế độ hỗ trợ khẩn cấp. Tình hình hiện tại: ${situation.summary} ${userProfile.capitalizedUserPronoun} hãy đứng yên tại vị trí an toàn."
            }
            DialogueIntent.FOLLOW_UP -> {
                val previousTurn = conversationHistory.lastOrNull()
                val contextNote = if (previousTurn != null) "Cập nhật tiếp theo: " else ""
                "$contextNote${situation.summary}"
            }
            DialogueIntent.SMALLTALK -> {
                generateSmalltalkReply(rawInput)
            }
            DialogueIntent.GENERAL -> {
                "${userProfile.capitalizedAiPronoun} đã ghi nhận. Quan sát hiện tại cho thấy: ${situation.summary} ${userProfile.capitalizedUserPronoun} có thể hỏi ${userProfile.aiPronoun} về độ an toàn, vật cản phía trước hoặc đọc chữ."
            }
        }
    }

    private fun generateSafetyReply(
        situation: SituationClassificationResult,
        snapshot: SceneSnapshot
    ): String {
        return when (situation.state) {
            SituationState.EMERGENCY_HAZARD -> {
                "Tuyệt đối chưa an toàn! ${situation.summary} ${userProfile.capitalizedUserPronoun} hãy dừng lại ngay."
            }
            SituationState.STREET_TRAFFIC -> {
                val vehicle = snapshot.objects.firstOrNull { it.label.containsAny(VEHICLE_KEYWORDS) }
                val position = vehicle?.direction?.vietnameseLabel ?: "phía trước"
                "Chưa an toàn để di chuyển: Có xe ở $position. ${userProfile.capitalizedUserPronoun} hãy chú ý lắng nghe và quan sát trước khi bước tiếp."
            }
            SituationState.INDOOR_NAVIGATION -> {
                val hazard = snapshot.objects.firstOrNull { it.label.containsAny(HAZARD_KEYWORDS) }
                if (hazard != null) {
                    "Cần cẩn thận: Có ${hazard.vietnameseLabel} ${hazard.direction.vietnameseLabel}. ${userProfile.capitalizedUserPronoun} hãy bước chậm lại."
                } else {
                    "Khu vực trong nhà tương đối an toàn, có một vài vật dụng nhưng không gây nguy hiểm lớn."
                }
            }
            SituationState.SOCIAL_MEETING,
            SituationState.READING_SIGNAGE,
            SituationState.SAFE_QUIET -> {
                "Khu vực hiện tại rất an toàn. Không có xe cộ hay mối nguy hiểm nào phía trước, ${userProfile.userPronoun} có thể yên tâm."
            }
        }
    }

    private fun generateSurroundingsReply(snapshot: SceneSnapshot): String {
        val objectList = if (snapshot.objects.isNotEmpty()) {
            snapshot.objects.take(3).joinToString(", ") {
                val near = if (it.isNear) "ở gần" else ""
                "${it.vietnameseLabel} ${it.direction.vietnameseLabel} $near".trim()
            }
        } else {
            "không có vật cản lớn"
        }

        val soundInfo = if (snapshot.sounds.isNotEmpty()) {
            val sound = snapshot.sounds.first()
            ", âm thanh ghi nhận được là ${sound.vietnameseLabel}"
        } else {
            ""
        }

        return "Phía trước ${userProfile.userPronoun} có $objectList$soundInfo. Mức độ ồn là ${snapshot.ambientDecibels.toInt()} đề-xi-ben."
    }

    private fun generateObjectIdentificationReply(snapshot: SceneSnapshot): String {
        val target = snapshot.objects.firstOrNull { it.direction == com.sensebridge.core.model.SpatialDirection.CENTER && it.isNear }
            ?: snapshot.objects.firstOrNull { it.direction == com.sensebridge.core.model.SpatialDirection.CENTER }
            ?: snapshot.objects.firstOrNull { it.isNear }
            ?: snapshot.objects.firstOrNull()

        if (target != null) {
            val dirDesc = target.direction.vietnameseLabel
            val nearDesc = if (target.isNear) "ở khoảng cách gần" else "ở khoảng cách vừa phải"
            val base = "Trước mặt ${userProfile.userPronoun} là ${target.vietnameseLabel} ($dirDesc), $nearDesc."
            return if (!snapshot.latestText.isNullOrBlank()) {
                "$base Trên vật thể có dòng chữ: \"${snapshot.latestText}\"."
            } else {
                base
            }
        }

        if (!snapshot.latestText.isNullOrBlank()) {
            return "Camera chưa nhận diện rõ hình dạng đồ vật, nhưng đọc được dòng chữ: \"${snapshot.latestText}\"."
        }

        return "Camera chưa quét thấy vật thể nào rõ ràng ở vị trí này. ${userProfile.capitalizedUserPronoun} hãy hướng ống kính lại gần hơn một chút nhé."
    }

    private fun generateSoundReply(snapshot: SceneSnapshot): String {
        return if (snapshot.sounds.isNotEmpty()) {
            val sounds = snapshot.sounds.take(2).joinToString(" và ") { it.vietnameseLabel }
            "Vừa nghe thấy $sounds. Mức âm lượng môi trường là ${snapshot.ambientDecibels.toInt()} đề-xi-ben."
        } else {
            val note = if (snapshot.isNoisy) "tiếng ồn môi trường khoảng ${snapshot.ambientDecibels.toInt()} dB" else "khá yên tĩnh"
            "Không phát hiện âm thanh đột biến nào, xung quanh hiện đang $note."
        }
    }

    private fun generateSmalltalkReply(rawInput: String): String {
        val semanticMatch = semanticMatcher.matchSubIntent(rawInput)
        return personaNlgEngine.generateSmalltalk(semanticMatch.act, userProfile)
    }

    private fun determineResponsePriority(
        intent: DialogueIntent,
        situation: SituationClassificationResult
    ): PriorityLevel {
        if (intent == DialogueIntent.HELP_REQUEST || situation.priority == PriorityLevel.CRITICAL_P0) {
            return PriorityLevel.CRITICAL_P0
        }
        if (intent == DialogueIntent.SAFETY_CHECK && situation.state == SituationState.STREET_TRAFFIC) {
            return PriorityLevel.WARNING_P1
        }
        return PriorityLevel.ATTENTION_P2
    }

    private fun recordTurn(userInput: String, aiResponse: String, intent: DialogueIntent) {
        if (conversationHistory.size >= maxHistoryTurns) {
            conversationHistory.removeAt(0)
        }
        conversationHistory.add(
            DialogueTurn(
                userInput = userInput,
                aiResponse = aiResponse,
                intent = intent
            )
        )
    }

    private fun String.containsAny(csv: String): Boolean {
        val targets = csv.split(",")
        val words = this.lowercase().split(Regex("""[\s\p{Punct}]+""")).filter { it.isNotBlank() }
        return targets.any { target ->
            val cleanTarget = target.trim().lowercase()
            if (cleanTarget.isEmpty()) return@any false
            if (cleanTarget.contains(" ")) {
                this.contains(cleanTarget, ignoreCase = true)
            } else if (cleanTarget.length <= 3) {
                words.contains(cleanTarget)
            } else {
                this.contains(cleanTarget, ignoreCase = true)
            }
        }
    }
}
