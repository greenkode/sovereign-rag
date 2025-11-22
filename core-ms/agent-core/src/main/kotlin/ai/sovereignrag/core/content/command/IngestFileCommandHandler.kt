package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import ai.sovereignrag.content.dto.IngestResponse
import ai.sovereignrag.content.dto.IngestStatusResponse
import ai.sovereignrag.content.service.IngestService
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Command handler for file ingestion
 * Delegates async processing to IngestService with @Async methods
 */
@Component
class IngestFileCommandHandler(
    private val ingestService: IngestService
) : Command.Handler<IngestFileCommand, IngestResponse> {

    override fun handle(command: IngestFileCommand): IngestResponse {
        val filename = command.file.originalFilename ?: "unknown"
        logger.info { "Handling IngestFileCommand: $filename (${command.file.size} bytes)" }

        val taskId = "file_ingest_${System.currentTimeMillis()}_${UUID.randomUUID().hashCode()}"

        ingestService.tasks[taskId] = IngestStatusResponse(
            status = "processing",
            progress = "Processing file..."
        )

        // Process asynchronously using Spring @Async
        // SecurityContext is automatically propagated via DelegatingSecurityContextAsyncTaskExecutor
        ingestService.processFileAsync(
            taskId = taskId,
            file = command.file,
            title = command.title,
            url = command.url
        )

        return IngestResponse(
            status = "accepted",
            taskId = taskId,
            message = "File upload accepted, processing..."
        )
    }
}
