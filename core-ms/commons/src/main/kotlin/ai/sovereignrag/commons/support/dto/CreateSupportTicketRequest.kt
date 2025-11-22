package ai.sovereignrag.commons.support.dto

import ai.sovereignrag.commons.support.enumeration.TicketCategory
import ai.sovereignrag.commons.support.enumeration.TicketPriority

data class CreateSupportTicketRequest(
    val subject: String,
    val description: String?,
    val contactEmail: String,
    val contactName: String?,
    val priority: TicketPriority? = TicketPriority.MEDIUM,
    val category: TicketCategory? = TicketCategory.GENERAL_INQUIRY,
    val customFields: Map<String, Any>? = null
)
