package ai.sovereignrag.ingestion.commons.repository

import ai.sovereignrag.ingestion.commons.entity.EvaluationStatus
import ai.sovereignrag.ingestion.commons.entity.RemiMetrics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface RemiMetricsRepository : JpaRepository<RemiMetrics, UUID> {

    fun findByQueryId(queryId: UUID): RemiMetrics?

    fun findByRetrievalMetricsId(retrievalMetricsId: UUID): RemiMetrics?

    @Query("""
        SELECT r FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.evaluatedAt >= :since
        ORDER BY r.evaluatedAt DESC
    """)
    fun findByOrganizationIdAndKnowledgeBaseId(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant,
        pageable: Pageable
    ): Page<RemiMetrics>

    @Query("""
        SELECT r FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.hallucinationDetected = true
        AND r.evaluatedAt >= :since
        ORDER BY r.evaluatedAt DESC
    """)
    fun findHallucinations(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant,
        pageable: Pageable
    ): Page<RemiMetrics>

    @Query("""
        SELECT r FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.missingKnowledgeDetected = true
        AND r.evaluatedAt >= :since
        ORDER BY r.evaluatedAt DESC
    """)
    fun findMissingKnowledge(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant,
        pageable: Pageable
    ): Page<RemiMetrics>

    @Query("""
        SELECT r FROM RemiMetrics r
        WHERE r.evaluationStatus = :status
        ORDER BY r.createdAt ASC
    """)
    fun findByEvaluationStatus(
        status: EvaluationStatus,
        pageable: Pageable
    ): Page<RemiMetrics>

    @Query("""
        SELECT AVG(r.overallScore) FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.evaluationStatus = 'COMPLETED'
        AND r.evaluatedAt >= :since
    """)
    fun getAverageOverallScore(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant
    ): Double?

    @Query("""
        SELECT AVG(r.answerRelevanceScore) FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.evaluationStatus = 'COMPLETED'
        AND r.evaluatedAt >= :since
    """)
    fun getAverageAnswerRelevance(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant
    ): Double?

    @Query("""
        SELECT AVG(r.contextRelevanceScore) FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.evaluationStatus = 'COMPLETED'
        AND r.evaluatedAt >= :since
    """)
    fun getAverageContextRelevance(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant
    ): Double?

    @Query("""
        SELECT AVG(r.groundednessScore) FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.evaluationStatus = 'COMPLETED'
        AND r.evaluatedAt >= :since
    """)
    fun getAverageGroundedness(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant
    ): Double?

    @Query("""
        SELECT COUNT(r) FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.hallucinationDetected = true
        AND r.evaluatedAt >= :since
    """)
    fun countHallucinations(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant
    ): Long

    @Query("""
        SELECT COUNT(r) FROM RemiMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.missingKnowledgeDetected = true
        AND r.evaluatedAt >= :since
    """)
    fun countMissingKnowledge(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant
    ): Long
}
