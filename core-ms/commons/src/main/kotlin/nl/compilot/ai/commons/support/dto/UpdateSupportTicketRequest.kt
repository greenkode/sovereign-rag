package nl.compilot.ai.commons.support.dto

import nl.compilot.ai.commons.support.enumeration.TicketCategory
import nl.compilot.ai.commons.support.enumeration.TicketPriority
import nl.compilot.ai.commons.support.enumeration.TicketStatus

data class UpdateSupportTicketRequest(
    val subject: String? = null,
    val description: String? = null,
    val status: TicketStatus? = null,
    val priority: TicketPriority? = null,
    val category: TicketCategory? = null,
    val assigneeEmail: String? = null,
    val customFields: Map<String, Any>? = null
)
