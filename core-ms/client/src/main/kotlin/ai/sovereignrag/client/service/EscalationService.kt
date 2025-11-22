package ai.sovereignrag.client.service

import mu.KotlinLogging
import nl.compilot.ai.client.domain.Escalation
import nl.compilot.ai.client.repository.EscalationRepository
import nl.compilot.ai.commons.EscalationLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class EscalationService(
    private val escalationRepository: EscalationRepository
) : EscalationLogger {

    override fun logEscalation(
        sessionId: UUID,
        reason: String,
        userEmail: String,
        userName: String,
        userPhone: String?,
        userMessage: String?,
        language: String?,
        persona: String,
        emailSent: Boolean
    ): UUID {
        try {
            val escalation = Escalation(
                sessionId = sessionId,
                reason = reason,
                userEmail = userEmail,
                userName = userName,
                userPhone = userPhone,
                userMessage = userMessage,
                language = language,
                persona = persona,
                emailSent = emailSent,
                status = "open",
                reviewed = false
            )

            val saved = escalationRepository.save(escalation)
            logger.info { "Logged escalation: ${saved.id} for user: $userEmail" }
            return saved.id
        } catch (e: Exception) {
            logger.error(e) { "Failed to log escalation for user: $userEmail" }
            throw e
        }
    }

    fun getAllEscalations(): List<Escalation> {
        logger.debug { "Fetching all escalations" }
        return escalationRepository.findAll()
    }

    fun getEscalationsByStatus(status: String): List<Escalation> {
        logger.debug { "Fetching escalations with status: $status" }
        return escalationRepository.findByStatusOrderByCreatedAtDesc(status)
    }

    fun markAsReviewed(
        id: UUID,
        reviewedBy: String,
        status: String,
        notes: String?
    ) {
        logger.info { "Marking escalation $id as reviewed by $reviewedBy" }

        escalationRepository.markAsReviewed(
            id = id,
            reviewedAt = Instant.now(),
            reviewedBy = reviewedBy,
            status = status,
            notes = notes
        )
    }

    fun getEscalationStatistics(): Map<String, Any> {
        logger.debug { "Fetching escalation statistics" }

        val totalEscalations = escalationRepository.count()
        val unreviewedCount = escalationRepository.countByReviewed(false)
        val reviewedCount = escalationRepository.countByReviewed(true)

        return mapOf(
            "totalEscalations" to totalEscalations,
            "unreviewedCount" to unreviewedCount,
            "reviewedCount" to reviewedCount
        )
    }
}
