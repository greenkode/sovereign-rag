package nl.compilot.ai.client.service

import mu.KotlinLogging
import nl.compilot.ai.client.domain.UnansweredQuery
import nl.compilot.ai.client.dto.UnansweredQueryDto
import nl.compilot.ai.client.repository.UnansweredQueryRepository
import nl.compilot.ai.commons.UnansweredQueryLogger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class UnansweredQueryService(
    private val unansweredQueryRepository: UnansweredQueryRepository
) : UnansweredQueryLogger {

    override fun logUnansweredQuery(
        query: String,
        response: String?,
        bestConfidence: Double?,
        usedGeneralKnowledge: Boolean,
        language: String?
    ) {
        try {
            // Use atomic upsert to handle concurrent/async operations safely
            // This avoids optimistic locking failures and detached entity issues
            unansweredQueryRepository.upsertUnansweredQuery(
                query = query,
                response = response,
                confidenceScore = bestConfidence,
                usedGeneralKnowledge = usedGeneralKnowledge,
                language = language
            )
            logger.debug { "Logged/updated unanswered query: ${query.take(50)}..." }
        } catch (e: Exception) {
            logger.error(e) { "Failed to log unanswered query" }
        }
    }

    fun getAllQueries(): List<UnansweredQuery> {
        logger.debug { "Fetching all open unanswered queries" }
        return unansweredQueryRepository.findByStatusOrderByOccurrenceCountDescLastOccurredAtDesc("open")
    }

    fun getQueriesPaginated(status: String, pageable: Pageable): Page<UnansweredQueryDto> {
        logger.debug { "Fetching paginated queries with status: $status, page: ${pageable.pageNumber}" }
        return unansweredQueryRepository.findByStatus(status, pageable)
            .map { UnansweredQueryDto.from(it) }
    }

    fun getQueryStatistics(): Map<String, Any> {
        logger.debug { "Fetching query statistics" }

        val totalQueries = unansweredQueryRepository.count()
        val openCount = unansweredQueryRepository.countByStatus("open")
        val resolvedCount = unansweredQueryRepository.countByStatus("resolved")

        // Calculate average confidence
        val allQueries = unansweredQueryRepository.findAll()
        val avgConfidence = allQueries
            .mapNotNull { it.confidenceScore }
            .takeIf { it.isNotEmpty() }
            ?.average()

        return mapOf(
            "totalQueries" to totalQueries,
            "openQueries" to openCount,
            "resolvedQueries" to resolvedCount,
            "averageConfidence" to (avgConfidence ?: 0.0)
        )
    }

    fun deleteQuery(id: java.util.UUID) {
        logger.info { "Deleting unanswered query: $id" }
        try {
            unansweredQueryRepository.deleteById(id)
            logger.info { "Successfully deleted unanswered query: $id" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete unanswered query: $id" }
            throw e
        }
    }

    fun markAsReviewed(id: java.util.UUID, notes: String? = null) {
        logger.info { "Marking unanswered query as reviewed: $id" }
        try {
            val query = unansweredQueryRepository.findById(id).orElseThrow {
                IllegalArgumentException("Query not found: $id")
            }

            val updatedQuery = query.copy(
                status = "reviewed",
                resolvedAt = java.time.Instant.now(),
                resolutionNotes = notes
            )

            unansweredQueryRepository.save(updatedQuery)
            logger.info { "Successfully marked unanswered query as reviewed: $id" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to mark unanswered query as reviewed: $id" }
            throw e
        }
    }
}
