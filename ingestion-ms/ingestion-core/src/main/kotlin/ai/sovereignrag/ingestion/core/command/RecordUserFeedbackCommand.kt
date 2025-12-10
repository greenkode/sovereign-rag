package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.core.query.RetrievalMetricResult
import an.awesome.pipelinr.Command
import java.util.UUID

data class RecordUserFeedbackCommand(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val queryId: UUID,
    val score: Double,
    val clickedIndex: Int?,
    val relevanceRatings: Map<Int, Int>?
) : Command<RetrievalMetricResult?>
