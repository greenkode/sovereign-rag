package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import ai.sovereignrag.content.dto.DeleteDocumentResponse
import ai.sovereignrag.content.service.ContentService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DeleteDocumentCommandHandler(
    private val contentService: ContentService
) : Command.Handler<DeleteDocumentCommand, DeleteDocumentResponse> {

    override fun handle(command: DeleteDocumentCommand): DeleteDocumentResponse {
        logger.info { "Handling DeleteDocumentCommand for URL: ${command.url}" }

        return try {
            contentService.deleteByUrl(command.url)
            logger.info { "Document deleted successfully: ${command.url}" }

            DeleteDocumentResponse(
                success = true,
                message = "Content deleted successfully"
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete content for URL: ${command.url}" }

            DeleteDocumentResponse(
                success = false,
                message = "Failed to delete content: ${e.message}"
            )
        }
    }
}
