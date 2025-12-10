package ai.sovereignrag.ingestion.commons.repository

import ai.sovereignrag.ingestion.commons.entity.MetricSource
import ai.sovereignrag.ingestion.commons.entity.QualityMetrics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface QualityMetricsRepository : JpaRepository<QualityMetrics, UUID> {

    fun findByIngestionJobId(ingestionJobId: UUID): QualityMetrics?

    @Query("""
        SELECT q FROM QualityMetrics q
        WHERE q.organizationId = :organizationId
        AND q.knowledgeBaseId = :knowledgeBaseId
        AND q.evaluatedAt >= :since
        ORDER BY q.evaluatedAt DESC
    """)
    fun findByOrganizationIdAndKnowledgeBaseId(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        since: Instant,
        pageable: Pageable
    ): Page<QualityMetrics>

    @Query("""
        SELECT q FROM QualityMetrics q
        WHERE q.organizationId = :organizationId
        AND q.knowledgeBaseId = :knowledgeBaseId
        AND q.metricSource = :metricSource
        AND q.evaluatedAt >= :since
        ORDER BY q.evaluatedAt DESC
    """)
    fun findByOrganizationIdAndKnowledgeBaseIdAndMetricSource(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        metricSource: MetricSource,
        since: Instant,
        pageable: Pageable
    ): Page<QualityMetrics>
}
