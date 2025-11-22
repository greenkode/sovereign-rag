package ai.sovereignrag.support.zoho.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ZohoTicketResponse(
    val id: String,
    val ticketNumber: String?,
    val subject: String,
    val description: String?,
    val status: String,
    val priority: String?,
    val category: String?,
    val email: String,
    val contactId: String?,
    val assigneeId: String?,
    val createdTime: Instant?,
    val modifiedTime: Instant?,
    val closedTime: Instant?,
    val webUrl: String?,
    @JsonProperty("cf")
    val customFields: Map<String, Any>? = null
)
