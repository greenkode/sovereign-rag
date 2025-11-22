package ai.sovereignrag.support

import ai.sovereignrag.commons.support.dto.AddCommentRequest
import ai.sovereignrag.commons.support.dto.CommentResult
import ai.sovereignrag.commons.support.dto.CreateSupportTicketRequest
import ai.sovereignrag.commons.support.dto.SupportTicketDto
import ai.sovereignrag.commons.support.dto.UpdateSupportTicketRequest

interface SupportIntegration {

    fun getId(): String

    fun createTicket(request: CreateSupportTicketRequest): SupportTicketDto

    fun getTicket(ticketId: String): SupportTicketDto

    fun updateTicket(ticketId: String, request: UpdateSupportTicketRequest): SupportTicketDto

    fun addComment(ticketId: String, request: AddCommentRequest): CommentResult

    fun closeTicket(ticketId: String): SupportTicketDto
}