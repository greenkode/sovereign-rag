package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import nl.compilot.ai.content.dto.IngestStatusResponse
import nl.compilot.ai.content.service.IngestService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class GetIngestStatusQueryHandler(
    private val ingestService: IngestService
) : Command.Handler<GetIngestStatusQuery, IngestStatusResponse> {

    override fun handle(query: GetIngestStatusQuery): IngestStatusResponse {
        logger.debug { "Handling GetIngestStatusQuery for taskId: ${query.taskId}" }

        return ingestService.tasks[query.taskId] ?: IngestStatusResponse(
            status = "not_found",
            error = "Task not found"
        )
    }
}
