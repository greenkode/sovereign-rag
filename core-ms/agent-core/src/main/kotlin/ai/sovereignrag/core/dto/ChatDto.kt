package ai.sovereignrag.core.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChatStartRequest(
    val persona: String = "customer_service",
    val language: String? = null  // e.g., "en", "nl", "de", "fr"
)

data class ChatStartResponse(
    @JsonProperty("session_id")
    val sessionId: String,
    val greeting: String
)

data class ChatMessageRequest(
    val message: String,
    @JsonProperty("use_general_knowledge")
    val useGeneralKnowledge: Boolean = true
)

data class ChatMessageResponse(
    val response: String,
    val sources: List<String> = emptyList(),
    @JsonProperty("suggests_close")
    val suggestsClose: Boolean = false
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
