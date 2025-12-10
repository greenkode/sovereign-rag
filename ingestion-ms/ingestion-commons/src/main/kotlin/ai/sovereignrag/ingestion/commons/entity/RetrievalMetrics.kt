package ai.sovereignrag.ingestion.commons.entity

import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "retrieval_metrics", schema = "ingestion")
class RetrievalMetrics() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    var organizationId: UUID = UUID.randomUUID()

    var knowledgeBaseId: UUID? = null

    var queryId: UUID? = null

    var queryText: String? = null

    var queryEmbeddingTimeMs: Long = 0

    var searchTimeMs: Long = 0

    var totalTimeMs: Long = 0

    var resultsReturned: Int = 0

    var resultsRequested: Int = 0

    var topResultScore: Double? = null

    var averageResultScore: Double? = null

    var lowestResultScore: Double? = null

    var scoreVariance: Double? = null

    var distinctSourcesCount: Int = 0

    var userFeedbackScore: Double? = null

    var clickedResultIndex: Int? = null

    var resultRelevanceRatings: String? = null

    var embeddingModel: String? = null

    var searchStrategy: String? = null

    var queriedAt: Instant = Instant.now()

    constructor(
        organizationId: UUID,
        knowledgeBaseId: UUID?
    ) : this() {
        this.organizationId = organizationId
        this.knowledgeBaseId = knowledgeBaseId
        this.queriedAt = Instant.now()
    }
}
