package ai.sovereignrag.commons.support.dto

import ai.sovereignrag.commons.support.enumeration.TicketCategory
import ai.sovereignrag.commons.support.enumeration.TicketPriority
import ai.sovereignrag.commons.support.enumeration.TicketStatus

data class UpdateSupportTicketRequest(
    val subject: String? = null,
    val description: String? = null,
    val status: TicketStatus? = null,
    val priority: TicketPriority? = null,
    val category: TicketCategory? = null,
    val assigneeEmail: String? = null,
    val customFields: Map<String, Any>? = null
)
