package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.repository.RemiMetricsRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@Component
class GetRemiSummaryQueryHandler(
    private val remiMetricsRepository: RemiMetricsRepository
) : Command.Handler<GetRemiSummaryQuery, RemiSummaryResult> {

    @Transactional(readOnly = true)
    override fun handle(query: GetRemiSummaryQuery): RemiSummaryResult {
        val since = Instant.now().minus(query.days.toLong(), ChronoUnit.DAYS)

        log.debug {
            "Getting REMI summary for org=${query.organizationId}, kb=${query.knowledgeBaseId}, since=$since"
        }

        val avgOverall = remiMetricsRepository.getAverageOverallScore(
            query.organizationId, query.knowledgeBaseId, since
        )
        val avgAnswerRelevance = remiMetricsRepository.getAverageAnswerRelevance(
            query.organizationId, query.knowledgeBaseId, since
        )
        val avgContextRelevance = remiMetricsRepository.getAverageContextRelevance(
            query.organizationId, query.knowledgeBaseId, since
        )
        val avgGroundedness = remiMetricsRepository.getAverageGroundedness(
            query.organizationId, query.knowledgeBaseId, since
        )
        val hallucinationCount = remiMetricsRepository.countHallucinations(
            query.organizationId, query.knowledgeBaseId, since
        )
        val missingKnowledgeCount = remiMetricsRepository.countMissingKnowledge(
            query.organizationId, query.knowledgeBaseId, since
        )

        val totalPage = remiMetricsRepository.findByOrganizationIdAndKnowledgeBaseId(
            query.organizationId, query.knowledgeBaseId, since, PageRequest.of(0, 1)
        )
        val totalEvaluations = totalPage.totalElements

        val healthStatus = calculateHealthStatus(avgOverall, hallucinationCount, missingKnowledgeCount, totalEvaluations)

        return RemiSummaryResult(
            averageOverallScore = avgOverall,
            averageAnswerRelevance = avgAnswerRelevance,
            averageContextRelevance = avgContextRelevance,
            averageGroundedness = avgGroundedness,
            hallucinationCount = hallucinationCount,
            missingKnowledgeCount = missingKnowledgeCount,
            totalEvaluations = totalEvaluations,
            healthStatus = healthStatus
        )
    }

    private fun calculateHealthStatus(
        avgOverall: Double?,
        hallucinationCount: Long,
        missingKnowledgeCount: Long,
        totalEvaluations: Long
    ): RemiHealthStatus {
        if (totalEvaluations == 0L || avgOverall == null) {
            return RemiHealthStatus.NO_DATA
        }

        val hallucinationRate = hallucinationCount.toDouble() / totalEvaluations
        val missingKnowledgeRate = missingKnowledgeCount.toDouble() / totalEvaluations

        return when {
            avgOverall >= 0.85 && hallucinationRate < 0.05 && missingKnowledgeRate < 0.10 -> RemiHealthStatus.EXCELLENT
            avgOverall >= 0.70 && hallucinationRate < 0.10 && missingKnowledgeRate < 0.20 -> RemiHealthStatus.GOOD
            avgOverall >= 0.55 && hallucinationRate < 0.20 && missingKnowledgeRate < 0.35 -> RemiHealthStatus.FAIR
            avgOverall >= 0.40 && hallucinationRate < 0.35 -> RemiHealthStatus.POOR
            else -> RemiHealthStatus.CRITICAL
        }
    }
}
