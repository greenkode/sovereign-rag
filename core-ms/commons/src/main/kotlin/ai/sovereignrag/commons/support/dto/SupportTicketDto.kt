package ai.sovereignrag.commons.support.dto

import ai.sovereignrag.commons.support.enumeration.TicketCategory
import ai.sovereignrag.commons.support.enumeration.TicketPriority
import ai.sovereignrag.commons.support.enumeration.TicketStatus
import java.time.Instant

data class SupportTicketDto(
    val id: String,
    val ticketNumber: String?,
    val subject: String,
    val description: String?,
    val status: TicketStatus,
    val priority: TicketPriority?,
    val category: TicketCategory?,
    val contactEmail: String,
    val contactName: String?,
    val assigneeEmail: String?,
    val assigneeName: String?,
    val createdTime: Instant?,
    val modifiedTime: Instant?,
    val closedTime: Instant?,
    val webUrl: String?,
    val customFields: Map<String, Any>? = null
)
