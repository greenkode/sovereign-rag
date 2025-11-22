package nl.compilot.ai.support.zoho.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ZohoTicketRequest(
    val subject: String,
    val description: String?,
    val email: String,
    val contactId: String?,
    val priority: String?,
    val status: String?,
    val category: String?,
    val assigneeId: String?,
    @JsonProperty("cf")
    val customFields: Map<String, Any>? = null
)
