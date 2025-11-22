package ai.sovereignrag.support.zoho

import nl.compilot.ai.commons.support.dto.*
import nl.compilot.ai.commons.support.enumeration.TicketCategory
import nl.compilot.ai.commons.support.enumeration.TicketPriority
import nl.compilot.ai.commons.support.enumeration.TicketStatus
import ai.sovereignrag.support.SupportIntegration
import ai.sovereignrag.support.zoho.dto.ZohoCommentRequest
import ai.sovereignrag.support.zoho.dto.ZohoCommentResponse
import ai.sovereignrag.support.zoho.dto.ZohoContactRequest
import ai.sovereignrag.support.zoho.dto.ZohoContactResponse
import ai.sovereignrag.support.zoho.dto.ZohoTicketRequest
import ai.sovereignrag.support.zoho.dto.ZohoTicketResponse
import nl.compilot.ai.support.zoho.dto.*
import ai.sovereignrag.support.zoho.interceptor.ZohoAuthInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

const val ZOHO_SUPPORT = "ZOHO_SUPPORT"

@Component(ZOHO_SUPPORT)
@ConditionalOnProperty(prefix = "support.zoho", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class ZohoSupportIntegration(
    @Value("\${support.zoho.api-url}") private val apiUrl: String,
    @Value("\${support.zoho.auth-url}") private val authUrl: String,
    @Value("\${support.zoho.client-id}") private val clientId: String,
    @Value("\${support.zoho.client-secret}") private val clientSecret: String,
    @Value("\${support.zoho.refresh-token}") private val refreshToken: String,
    @Value("\${support.zoho.org-id}") private val orgId: String
) : SupportIntegration {

    private val restClient: RestClient

    init {
        val authInterceptor = ZohoAuthInterceptor(
            clientId = clientId,
            clientSecret = clientSecret,
            refreshToken = refreshToken,
            authUrl = authUrl
        )

        restClient = RestClient.builder()
            .baseUrl(apiUrl)
            .requestInterceptor(authInterceptor)
            .build()
    }

    override fun getId(): String = ZOHO_SUPPORT

    override fun createTicket(request: CreateSupportTicketRequest): SupportTicketDto {
        // First, find or create contact
        val contactId = findOrCreateContact(request.contactEmail, request.contactName)

        val zohoRequest = ZohoTicketRequest(
            subject = request.subject,
            description = request.description,
            email = request.contactEmail,
            contactId = contactId,
            priority = mapPriorityToZoho(request.priority),
            status = "Open",
            category = mapCategoryToZoho(request.category),
            assigneeId = null,
            customFields = request.customFields
        )

        val response = restClient.post()
            .uri("/api/v1/tickets?orgId=$orgId")
            .body(zohoRequest)
            .retrieve()
            .body<ZohoTicketResponse>()
            ?: throw RuntimeException("Failed to create ticket in Zoho")

        return mapToSupportTicketDto(response)
    }

    override fun getTicket(ticketId: String): SupportTicketDto {
        val response = restClient.get()
            .uri("/api/v1/tickets/$ticketId?orgId=$orgId")
            .retrieve()
            .body<ZohoTicketResponse>()
            ?: throw RuntimeException("Ticket not found: $ticketId")

        return mapToSupportTicketDto(response)
    }

    override fun updateTicket(ticketId: String, request: UpdateSupportTicketRequest): SupportTicketDto {
        val updateMap = mutableMapOf<String, Any?>()

        request.subject?.let { updateMap["subject"] = it }
        request.description?.let { updateMap["description"] = it }
        request.status?.let { updateMap["status"] = mapStatusToZoho(it) }
        request.priority?.let { updateMap["priority"] = mapPriorityToZoho(it) }
        request.category?.let { updateMap["category"] = mapCategoryToZoho(it) }
        request.assigneeEmail?.let {
            // In a real implementation, you'd look up assignee by email
            updateMap["assigneeId"] = it
        }
        request.customFields?.let { updateMap["cf"] = it }

        val response = restClient.patch()
            .uri("/api/v1/tickets/$ticketId?orgId=$orgId")
            .body(updateMap)
            .retrieve()
            .body<ZohoTicketResponse>()
            ?: throw RuntimeException("Failed to update ticket: $ticketId")

        return mapToSupportTicketDto(response)
    }

    override fun addComment(ticketId: String, request: AddCommentRequest): CommentResult {
        val zohoRequest = ZohoCommentRequest(
            content = request.content,
            isPublic = request.isPublic
        )

        val response = restClient.post()
            .uri("/api/v1/tickets/$ticketId/comments?orgId=$orgId")
            .body(zohoRequest)
            .retrieve()
            .body<ZohoCommentResponse>()
            ?: throw RuntimeException("Failed to add comment to ticket: $ticketId")

        return CommentResult(
            id = response.id,
            content = response.content,
            isPublic = response.isPublic,
            createdTime = response.createdTime,
            authorEmail = response.authorEmail,
            authorName = response.authorName
        )
    }

    override fun closeTicket(ticketId: String): SupportTicketDto {
        return updateTicket(
            ticketId,
            UpdateSupportTicketRequest(status = TicketStatus.CLOSED)
        )
    }

    private fun findOrCreateContact(email: String, name: String?): String? {
        return try {
            val response = restClient.get()
                .uri("/api/v1/contacts/search?email=$email&orgId=$orgId")
                .retrieve()
                .body<ZohoContactResponse>()

            response?.id
        } catch (e: Exception) {
            // Contact not found, create new one
            createContact(email, name)
        }
    }

    private fun createContact(email: String, name: String?): String? {
        val names = name?.split(" ", limit = 2) ?: listOf()
        val contactRequest = ZohoContactRequest(
            email = email,
            firstName = names.getOrNull(0),
            lastName = names.getOrNull(1)
        )

        val response = restClient.post()
            .uri("/api/v1/contacts?orgId=$orgId")
            .body(contactRequest)
            .retrieve()
            .body<ZohoContactResponse>()

        return response?.id
    }

    private fun mapToSupportTicketDto(zohoTicket: ZohoTicketResponse): SupportTicketDto {
        return SupportTicketDto(
            id = zohoTicket.id,
            ticketNumber = zohoTicket.ticketNumber,
            subject = zohoTicket.subject,
            description = zohoTicket.description,
            status = mapStatusFromZoho(zohoTicket.status),
            priority = zohoTicket.priority?.let { mapPriorityFromZoho(it) },
            category = zohoTicket.category?.let { mapCategoryFromZoho(it) },
            contactEmail = zohoTicket.email,
            contactName = null, // Would need to fetch from contact
            assigneeEmail = null, // Would need to fetch from assignee
            assigneeName = null,
            createdTime = zohoTicket.createdTime,
            modifiedTime = zohoTicket.modifiedTime,
            closedTime = zohoTicket.closedTime,
            webUrl = zohoTicket.webUrl,
            customFields = zohoTicket.customFields
        )
    }

    // Status mapping
    private fun mapStatusToZoho(status: TicketStatus): String = when (status) {
        TicketStatus.OPEN -> "Open"
        TicketStatus.IN_PROGRESS -> "In Progress"
        TicketStatus.WAITING_ON_CUSTOMER -> "Waiting on Customer"
        TicketStatus.WAITING_ON_THIRD_PARTY -> "Waiting on Third Party"
        TicketStatus.RESOLVED -> "Resolved"
        TicketStatus.CLOSED -> "Closed"
        TicketStatus.SPAM -> "Spam"
    }

    private fun mapStatusFromZoho(status: String): TicketStatus = when (status) {
        "Open" -> TicketStatus.OPEN
        "In Progress" -> TicketStatus.IN_PROGRESS
        "Waiting on Customer" -> TicketStatus.WAITING_ON_CUSTOMER
        "Waiting on Third Party" -> TicketStatus.WAITING_ON_THIRD_PARTY
        "Resolved" -> TicketStatus.RESOLVED
        "Closed" -> TicketStatus.CLOSED
        "Spam" -> TicketStatus.SPAM
        else -> TicketStatus.OPEN
    }

    // Priority mapping
    private fun mapPriorityToZoho(priority: TicketPriority?): String? = when (priority) {
        TicketPriority.LOW -> "Low"
        TicketPriority.MEDIUM -> "Medium"
        TicketPriority.HIGH -> "High"
        TicketPriority.URGENT -> "Urgent"
        null -> null
    }

    private fun mapPriorityFromZoho(priority: String): TicketPriority = when (priority) {
        "Low" -> TicketPriority.LOW
        "Medium" -> TicketPriority.MEDIUM
        "High" -> TicketPriority.HIGH
        "Urgent" -> TicketPriority.URGENT
        else -> TicketPriority.MEDIUM
    }

    // Category mapping
    private fun mapCategoryToZoho(category: TicketCategory?): String? = when (category) {
        TicketCategory.ACCOUNT_ISSUE -> "Account Issue"
        TicketCategory.TRANSACTION_DISPUTE -> "Transaction Dispute"
        TicketCategory.TECHNICAL_SUPPORT -> "Technical Support"
        TicketCategory.BILLING_INQUIRY -> "Billing Inquiry"
        TicketCategory.FEATURE_REQUEST -> "Feature Request"
        TicketCategory.GENERAL_INQUIRY -> "General Inquiry"
        null -> null
    }

    private fun mapCategoryFromZoho(category: String): TicketCategory = when (category) {
        "Account Issue" -> TicketCategory.ACCOUNT_ISSUE
        "Transaction Dispute" -> TicketCategory.TRANSACTION_DISPUTE
        "Technical Support" -> TicketCategory.TECHNICAL_SUPPORT
        "Billing Inquiry" -> TicketCategory.BILLING_INQUIRY
        "Feature Request" -> TicketCategory.FEATURE_REQUEST
        "General Inquiry" -> TicketCategory.GENERAL_INQUIRY
        else -> TicketCategory.GENERAL_INQUIRY
    }
}
