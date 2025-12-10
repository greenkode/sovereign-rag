package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.core.metrics.RetrievalMetricsService
import ai.sovereignrag.ingestion.core.query.RetrievalMetricResult
import ai.sovereignrag.ingestion.core.query.toResult
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RecordUserFeedbackCommandHandler(
    private val retrievalMetricsService: RetrievalMetricsService
) : Command.Handler<RecordUserFeedbackCommand, RetrievalMetricResult?> {

    override fun handle(command: RecordUserFeedbackCommand): RetrievalMetricResult? {
        log.info { "Recording user feedback for query ${command.queryId} in knowledge base ${command.knowledgeBaseId}" }

        return retrievalMetricsService.recordUserFeedback(
            organizationId = command.organizationId,
            knowledgeBaseId = command.knowledgeBaseId,
            queryId = command.queryId,
            feedbackScore = command.score,
            clickedResultIndex = command.clickedIndex,
            relevanceRatings = command.relevanceRatings
        )?.toResult()
    }
}
