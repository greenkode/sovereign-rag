package ai.sovereignrag.core.chat.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChatStartRequest(
    val persona: String = "customer_service",
    val language: String? = null,
    @JsonProperty("show_gk_disclaimer")
    val showGkDisclaimer: Boolean = false,
    @JsonProperty("gk_disclaimer_text")
    val gkDisclaimerText: String? = null
)

data class ChatStartResponse(
    @JsonProperty("session_id")
    val sessionId: String
)

data class ChatMessageRequest(
    val message: String,
    @JsonProperty("use_general_knowledge")
    val useGeneralKnowledge: Boolean = true,
    @JsonProperty("show_gk_disclaimer")
    val showGkDisclaimer: Boolean = false,
    @JsonProperty("gk_disclaimer_text")
    val gkDisclaimerText: String? = null,
    @JsonProperty("show_sources")
    val showSources: Boolean = true
)

data class ChatMessageResponse(
    val response: String,
    val sources: List<String> = emptyList(),
    @JsonProperty("suggests_close")
    val suggestsClose: Boolean = false,
    @JsonProperty("confidence_score")
    val confidenceScore: Int? = null,  // 0-100 percentage for progress bar display
    @JsonProperty("show_confidence")
    val showConfidence: Boolean = true  // Whether to display confidence score in UI
)

data class EscalationRequest(
    @JsonProperty("user_email")
    val userEmail: String,
    @JsonProperty("user_name")
    val userName: String? = null,
    @JsonProperty("user_phone")
    val userPhone: String? = null,
    @JsonProperty("user_message")
    val userMessage: String? = null,
    val reason: String = "User requested human support"
)

data class EscalationResponse(
    val success: Boolean,
    val message: String,
    @JsonProperty("escalation_id")
    val escalationId: String
)

/**
 * Result of chat interaction including response, sources, flags, and confidence score
 *
 * This is an internal result type used by ConversationalAgentService.
 * It's converted to ChatMessageResponse before being returned to the API layer.
 */
data class ChatInteractionResult(
    val response: String,
    val sources: List<String>,
    val userSeemsFinished: Boolean,
    val confidenceScore: Int?,  // 0-100 percentage
    val showConfidence: Boolean = true  // Whether to display confidence score in UI
)
