package ai.sovereignrag.ingestion.commons.entity

import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class MetricType {
    WRITE_TIME,
    READ_TIME
}

enum class MetricSource {
    CHUNKING,
    EMBEDDING,
    RETRIEVAL,
    USER_FEEDBACK
}

@Entity
@Table(name = "quality_metrics", schema = "ingestion")
class QualityMetrics() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    var organizationId: UUID = UUID.randomUUID()

    var knowledgeBaseId: UUID? = null

    var knowledgeSourceId: UUID? = null

    var ingestionJobId: UUID? = null

    @Enumerated(EnumType.STRING)
    var metricType: MetricType = MetricType.WRITE_TIME

    @Enumerated(EnumType.STRING)
    var metricSource: MetricSource = MetricSource.CHUNKING

    var overallScore: Double = 0.0

    var coherenceScore: Double? = null

    var boundaryScore: Double? = null

    var sizeDistributionScore: Double? = null

    var contextSufficiencyScore: Double? = null

    var informationPreservationScore: Double? = null

    var chunkCount: Int = 0

    var averageChunkSize: Double = 0.0

    var minChunkSize: Int? = null

    var maxChunkSize: Int? = null

    var chunkingStrategy: String? = null

    var embeddingModel: String? = null

    var processingTimeMs: Long = 0

    var evaluatedAt: Instant = Instant.now()

    constructor(
        organizationId: UUID,
        knowledgeBaseId: UUID?,
        metricType: MetricType,
        metricSource: MetricSource
    ) : this() {
        this.organizationId = organizationId
        this.knowledgeBaseId = knowledgeBaseId
        this.metricType = metricType
        this.metricSource = metricSource
        this.evaluatedAt = Instant.now()
    }
}
