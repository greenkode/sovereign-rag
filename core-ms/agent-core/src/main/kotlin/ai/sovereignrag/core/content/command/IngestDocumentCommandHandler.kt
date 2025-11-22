package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import nl.compilot.ai.content.dto.IngestResponse
import nl.compilot.ai.content.dto.IngestStatusResponse
import nl.compilot.ai.content.service.IngestService
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Command handler for document ingestion
 * Delegates async processing to IngestService with @Async methods
 */
@Component
class IngestDocumentCommandHandler(
    private val ingestService: IngestService
) : Command.Handler<IngestDocumentCommand, IngestResponse> {

    override fun handle(command: IngestDocumentCommand): IngestResponse {
        logger.info { "Handling IngestDocumentCommand for: ${command.title}" }

        val taskId = "ingest_${System.currentTimeMillis()}_${UUID.randomUUID().hashCode()}"

        // Update task status
        ingestService.tasks[taskId] = IngestStatusResponse(
            status = "processing",
            progress = "Starting ingestion..."
        )

        // Process asynchronously using Spring @Async
        // SecurityContext is automatically propagated via DelegatingSecurityContextAsyncTaskExecutor
        ingestService.processDocumentAsync(
            taskId = taskId,
            title = command.title,
            content = command.content,
            url = command.url,
            postType = command.postType,
            siteTitle = command.siteTitle,
            siteTagline = command.siteTagline,
            category = command.category,
            categoryDescription = command.categoryDescription,
            tags = command.tags,
            excerpt = command.excerpt,
            author = command.author,
            authorBio = command.authorBio,
            relatedPosts = command.relatedPosts,
            breadcrumb = command.breadcrumb
        )

        return IngestResponse(
            status = "accepted",
            taskId = taskId,
            message = "Ingestion task started"
        )
    }
}
