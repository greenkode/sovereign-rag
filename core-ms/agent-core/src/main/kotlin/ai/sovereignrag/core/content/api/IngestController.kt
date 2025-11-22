package ai.sovereignrag.core.content.api

import an.awesome.pipelinr.Pipeline
import mu.KotlinLogging
import ai.sovereignrag.content.command.DeleteDocumentCommand
import ai.sovereignrag.content.command.IngestDocumentCommand
import ai.sovereignrag.content.command.IngestFileCommand
import ai.sovereignrag.content.dto.DeleteDocumentResponse
import ai.sovereignrag.content.dto.IngestRequest
import ai.sovereignrag.content.dto.IngestResponse
import ai.sovereignrag.content.dto.IngestStatusResponse
import ai.sovereignrag.content.query.GetIngestStatusQuery
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class IngestController(
    private val pipeline: Pipeline
) {

    @PostMapping("/ingest")
    fun ingest(@RequestBody request: IngestRequest): IngestResponse {
        logger.info { "Ingest request for: ${request.title}" }

        val command = IngestDocumentCommand(
            title = request.title,
            content = request.content,
            url = request.url,
            postType = request.postType,
            date = request.date,
            siteTitle = request.siteTitle,
            siteTagline = request.siteTagline,
            category = request.category,
            categoryDescription = request.categoryDescription,
            tags = request.tags,
            excerpt = request.excerpt,
            author = request.author,
            authorBio = request.authorBio,
            relatedPosts = request.relatedPosts,
            breadcrumb = request.breadcrumb
        )

        return pipeline.send(command)
    }

    @PostMapping("/ingest-file")
    fun ingestFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("title", required = false) title: String?,
        @RequestParam("url", required = false) url: String?
    ): IngestResponse {
        val filename = file.originalFilename ?: "unknown"
        logger.info { "File upload request: $filename (${file.size} bytes)" }

        val command = IngestFileCommand(
            file = file,
            title = title,
            url = url
        )

        return pipeline.send(command)
    }

    @GetMapping("/ingest/status/{taskId}")
    fun getStatus(@PathVariable taskId: String): IngestStatusResponse {
        logger.debug { "Get ingest status for taskId: $taskId" }

        val query = GetIngestStatusQuery(taskId = taskId)

        return pipeline.send(query)
    }

    @DeleteMapping("/ingest")
    fun delete(@RequestParam url: String): DeleteDocumentResponse {
        logger.info { "Delete request for URL: $url" }

        val command = DeleteDocumentCommand(url = url)

        return pipeline.send(command)
    }

    @PostMapping("/spell-dictionary/rebuild")
    fun rebuildSpellDictionary(): Map<String, String> {
        logger.info { "Spell dictionary rebuild endpoint called" }

        return mapOf(
            "status" to "disabled",
            "message" to "Spell correction service has been removed"
        )
    }
}
