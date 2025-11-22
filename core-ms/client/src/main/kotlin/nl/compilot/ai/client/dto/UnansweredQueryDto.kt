package nl.compilot.ai.client.dto

import nl.compilot.ai.client.domain.UnansweredQuery
import java.time.Instant
import java.util.UUID

/**
 * DTO for unanswered queries to avoid exposing entity directly to frontend
 */
data class UnansweredQueryDto(
    val id: UUID,
    val query: String,
    val response: String?,
    val language: String?,
    val confidenceScore: Double?,
    val status: String,
    val usedGeneralKnowledge: Boolean,
    val resolvedAt: Instant?,
    val resolutionNotes: String?,
    val createdAt: Instant,
    val occurrenceCount: Int,
    val lastOccurredAt: Instant
) {
    companion object {
        fun from(entity: UnansweredQuery): UnansweredQueryDto {
            return UnansweredQueryDto(
                id = entity.id,
                query = entity.query,
                response = entity.response,
                language = entity.language,
                confidenceScore = entity.confidenceScore,
                status = entity.status,
                usedGeneralKnowledge = entity.usedGeneralKnowledge,
                resolvedAt = entity.resolvedAt,
                resolutionNotes = entity.resolutionNotes,
                createdAt = entity.createdAt,
                occurrenceCount = entity.occurrenceCount,
                lastOccurredAt = entity.lastOccurredAt
            )
        }
    }
}
