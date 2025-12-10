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

enum class EvaluationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

@Entity
@Table(name = "remi_metrics", schema = "ingestion")
class RemiMetrics() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    var organizationId: UUID = UUID.randomUUID()

    var knowledgeBaseId: UUID = UUID.randomUUID()

    var queryId: UUID = UUID.randomUUID()

    var retrievalMetricsId: UUID? = null

    var queryText: String = ""

    var generatedAnswer: String? = null

    var answerRelevanceScore: Double? = null

    var answerRelevanceReasoning: String? = null

    var contextRelevanceScore: Double? = null

    var contextRelevanceReasoning: String? = null

    var groundednessScore: Double? = null

    var groundednessReasoning: String? = null

    var overallScore: Double? = null

    var retrievedChunksCount: Int = 0

    var evaluatedChunksCount: Int = 0

    var hallucinationDetected: Boolean = false

    var missingKnowledgeDetected: Boolean = false

    var evaluationModel: String? = null

    var evaluationTimeMs: Long = 0

    @Enumerated(EnumType.STRING)
    var evaluationStatus: EvaluationStatus = EvaluationStatus.PENDING

    var errorMessage: String? = null

    var evaluatedAt: Instant? = null

    constructor(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        queryId: UUID,
        queryText: String
    ) : this() {
        this.organizationId = organizationId
        this.knowledgeBaseId = knowledgeBaseId
        this.queryId = queryId
        this.queryText = queryText
    }

    fun calculateOverallScore() {
        val scores = listOfNotNull(answerRelevanceScore, contextRelevanceScore, groundednessScore)
        overallScore = scores.takeIf { it.isNotEmpty() }?.average()
    }

    fun markCompleted() {
        evaluationStatus = EvaluationStatus.COMPLETED
        evaluatedAt = Instant.now()
        calculateOverallScore()

        hallucinationDetected = groundednessScore?.let { it < 0.5 } ?: false
        missingKnowledgeDetected = contextRelevanceScore?.let { it < 0.3 } ?: false
    }

    fun markFailed(error: String) {
        evaluationStatus = EvaluationStatus.FAILED
        errorMessage = error
        evaluatedAt = Instant.now()
    }
}
