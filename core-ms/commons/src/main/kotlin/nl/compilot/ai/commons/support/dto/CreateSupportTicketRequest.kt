package nl.compilot.ai.commons.support.dto

import nl.compilot.ai.commons.support.enumeration.TicketCategory
import nl.compilot.ai.commons.support.enumeration.TicketPriority

data class CreateSupportTicketRequest(
    val subject: String,
    val description: String?,
    val contactEmail: String,
    val contactName: String?,
    val priority: TicketPriority? = TicketPriority.MEDIUM,
    val category: TicketCategory? = TicketCategory.GENERAL_INQUIRY,
    val customFields: Map<String, Any>? = null
)
