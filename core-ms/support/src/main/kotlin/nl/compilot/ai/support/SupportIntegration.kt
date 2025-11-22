package nl.compilot.ai.support

import nl.compilot.ai.commons.support.dto.AddCommentRequest
import nl.compilot.ai.commons.support.dto.CommentResult
import nl.compilot.ai.commons.support.dto.CreateSupportTicketRequest
import nl.compilot.ai.commons.support.dto.SupportTicketDto
import nl.compilot.ai.commons.support.dto.UpdateSupportTicketRequest

interface SupportIntegration {

    fun getId(): String

    fun createTicket(request: CreateSupportTicketRequest): SupportTicketDto

    fun getTicket(ticketId: String): SupportTicketDto

    fun updateTicket(ticketId: String, request: UpdateSupportTicketRequest): SupportTicketDto

    fun addComment(ticketId: String, request: AddCommentRequest): CommentResult

    fun closeTicket(ticketId: String): SupportTicketDto
}
