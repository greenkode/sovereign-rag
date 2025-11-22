package ai.sovereignrag.core.content.service

import mu.KotlinLogging
import nl.compilot.ai.commons.tenant.TenantContext
import nl.compilot.ai.content.dto.IngestStatusResponse
import nl.compilot.ai.content.event.ContentEventPublisher
import nl.compilot.ai.content.event.ContentIngestionEvent
import nl.compilot.ai.domain.ContentDocument
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Service for asynchronous content ingestion
 * Methods are annotated with @Async and will be executed in background threads
 * SecurityContext is automatically propagated via DelegatingSecurityContextAsyncTaskExecutor
 */
@Service
class IngestService(
    private val contentService: ContentService,
    private val contentEventPublisher: ContentEventPublisher
) {

    // Shared task tracking across all ingestion operations
    val tasks = ConcurrentHashMap<String, IngestStatusResponse>()

    /**
     * Process document ingestion asynchronously
     * SecurityContext is automatically propagated by Spring's @Async
     */
    @Async
    fun processDocumentAsync(
        taskId: String,
        title: String,
        content: String,
        url: String,
        postType: String,
        siteTitle: String?,
        siteTagline: String?,
        category: String?,
        categoryDescription: String?,
        tags: List<String>?,
        excerpt: String?,
        author: String?,
        authorBio: String?,
        relatedPosts: List<String>?,
        breadcrumb: String?
    ) {
        try {
            logger.info { "Processing document asynchronously: $title" }

            val tenantId = TenantContext.getCurrentTenantOrNull()

            // Delete any existing content with this URL (handles updates)
            try {
                contentService.deleteByUrl(url)
            } catch (e: Exception) {
                logger.debug { "No existing content to delete for URL: $url" }
            }

            // Generate deterministic ID from URL to ensure idempotency
            val contentId = UUID.nameUUIDFromBytes(url.toByteArray()).toString()

            // Build enriched metadata (all values as strings)
            val metadata = buildMap<String, String> {
                put("post_type", postType)
                siteTitle?.let { put("site_title", it) }
                siteTagline?.let { put("site_tagline", it) }
                category?.let { put("category", it) }
                categoryDescription?.let { put("category_description", it) }
                tags?.let { put("tags", it.joinToString(",")) }
                excerpt?.let { put("excerpt", it) }
                author?.let { put("author", it) }
                authorBio?.let { put("author_bio", it) }
                relatedPosts?.let { put("related_posts", it.joinToString(",")) }
                breadcrumb?.let { put("breadcrumb", it) }
            }

            val contentDoc = ContentDocument(
                id = contentId,
                title = title,
                content = content,
                url = url,
                source = url,
                createdAt = LocalDateTime.now(),
                metadata = metadata
            )

            contentService.ingest(contentDoc)

            // Publish event for contextual enrichment
            contentEventPublisher.publishIngestionEvent(
                ContentIngestionEvent(
                    tenantId = tenantId ?: "unknown",
                    documentId = UUID.fromString(contentId),
                    title = title,
                    category = category,
                    postType = postType
                )
            )

            tasks[taskId] = IngestStatusResponse(
                status = "completed",
                progress = "Ingestion completed successfully"
            )

            logger.info { "Document ingested successfully: $title" }
        } catch (e: Exception) {
            logger.error(e) { "Ingestion failed for task $taskId" }
            tasks[taskId] = IngestStatusResponse(
                status = "failed",
                error = e.message
            )
        }
    }

    /**
     * Process file ingestion asynchronously
     * SecurityContext is automatically propagated by Spring's @Async
     */
    @Async
    fun processFileAsync(
        taskId: String,
        file: MultipartFile,
        title: String?,
        url: String?
    ) {
        try {
            val filename = file.originalFilename ?: "unknown"
            logger.info { "Processing file asynchronously: $filename" }

            // Extract text from file
            val extractedText = contentService.extractTextFromFile(file)

            val finalTitle = title?.takeIf { it.isNotBlank() } ?: filename
            val finalUrl = url?.takeIf { it.isNotBlank() }
                ?: "file-upload-${UUID.randomUUID()}"

            // Delete any existing content with this URL
            try {
                contentService.deleteByUrl(finalUrl)
            } catch (e: Exception) {
                logger.debug { "No existing content to delete for URL: $finalUrl" }
            }

            val contentId = UUID.nameUUIDFromBytes(finalUrl.toByteArray()).toString()

            val contentDoc = ContentDocument(
                id = contentId,
                title = finalTitle,
                content = extractedText,
                url = finalUrl,
                source = finalUrl,
                createdAt = LocalDateTime.now(),
                metadata = mapOf(
                    "post_type" to "file-upload",
                    "filename" to filename,
                    "file_size" to file.size.toString(),
                    "content_type" to (file.contentType ?: "unknown")
                )
            )

            contentService.ingest(contentDoc)

            tasks[taskId] = IngestStatusResponse(
                status = "completed",
                progress = "File processed and ingested successfully"
            )

            logger.info { "File ingested successfully: $filename" }
        } catch (e: Exception) {
            logger.error(e) { "File ingestion failed for task $taskId" }
            tasks[taskId] = IngestStatusResponse(
                status = "failed",
                error = "Failed to process file: ${e.message}"
            )
        }
    }
}
