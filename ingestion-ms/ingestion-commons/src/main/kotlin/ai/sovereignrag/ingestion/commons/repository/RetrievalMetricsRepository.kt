package ai.sovereignrag.ingestion.commons.repository

import ai.sovereignrag.ingestion.commons.entity.RetrievalMetrics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface RetrievalMetricsRepository : JpaRepository<RetrievalMetrics, UUID> {

    fun findByQueryId(queryId: UUID): RetrievalMetrics?

    @Query("""
        SELECT r FROM RetrievalMetrics r
        WHERE r.organizationId = :organizationId
        AND r.knowledgeBaseId = :knowledgeBaseId
        AND r.queriedAt >= :since
        ORDER BY r.queriedAt DESC
    """)
    fun findByOrganizationIdAndKnowledgeBaseId(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant,
        pageable: Pageable
    ): Page<RetrievalMetrics>
}
