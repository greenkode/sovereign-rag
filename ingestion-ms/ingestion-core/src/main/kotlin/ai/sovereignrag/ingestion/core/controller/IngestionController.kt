package ai.sovereignrag.ingestion.core.controller

import ai.sovereignrag.ingestion.commons.dto.BatchUploadRequest
import ai.sovereignrag.ingestion.commons.dto.BatchUploadResponse
import ai.sovereignrag.ingestion.commons.dto.ConfirmUploadRequest
import ai.sovereignrag.ingestion.commons.dto.FolderUploadRequest
import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.dto.JobListResponse
import ai.sovereignrag.ingestion.commons.dto.OrganizationQuotaResponse
import ai.sovereignrag.ingestion.commons.dto.PresignedUploadRequest
import ai.sovereignrag.ingestion.commons.dto.PresignedUploadResponse
import ai.sovereignrag.ingestion.commons.dto.QAPairsRequest
import ai.sovereignrag.ingestion.commons.dto.RssFeedRequest
import ai.sovereignrag.ingestion.commons.dto.TextInputRequest
import ai.sovereignrag.ingestion.commons.dto.WebScrapeRequest
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.core.command.BatchUploadCommand
import ai.sovereignrag.ingestion.core.command.CancelJobCommand
import ai.sovereignrag.ingestion.core.command.ConfirmBatchUploadCommand
import ai.sovereignrag.ingestion.core.command.ConfirmUploadCommand
import ai.sovereignrag.ingestion.core.command.CreatePresignedUploadCommand
import ai.sovereignrag.ingestion.core.command.FolderUploadCommand
import ai.sovereignrag.ingestion.core.command.RetryJobCommand
import ai.sovereignrag.ingestion.core.command.SubmitQAPairsCommand
import ai.sovereignrag.ingestion.core.command.SubmitRssFeedCommand
import ai.sovereignrag.ingestion.core.command.SubmitScrapeJobCommand
import ai.sovereignrag.ingestion.core.command.SubmitTextInputCommand
import ai.sovereignrag.ingestion.core.query.GetJobQuery
import ai.sovereignrag.ingestion.core.query.GetOrganizationQuotaQuery
import ai.sovereignrag.ingestion.core.query.ListJobsQuery
import ai.sovereignrag.ingestion.core.service.SecurityContextService
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/ingestion")
@Tag(name = "Ingestion", description = "File ingestion and processing endpoints")
@SecurityRequirement(name = "bearerAuth")
class IngestionController(
    private val pipeline: Pipeline,
    private val securityContextService: SecurityContextService
) {

    @PostMapping("/upload/presigned")
    @Operation(summary = "Get presigned upload URL", description = "Generate a presigned URL for direct upload to S3")
    fun getPresignedUploadUrl(@RequestBody request: PresignedUploadRequest): PresignedUploadResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Generating presigned URL for organization $organizationId, file: ${request.fileName}" }
        return pipeline.send(
            CreatePresignedUploadCommand(
                organizationId = organizationId,
                fileName = request.fileName,
                fileSize = request.fileSize,
                contentType = request.contentType,
                knowledgeBaseId = request.knowledgeBaseId
            )
        )
    }

    @PostMapping("/upload/confirm")
    @Operation(summary = "Confirm upload completion", description = "Confirm that file upload is complete and start processing")
    fun confirmUpload(@RequestBody request: ConfirmUploadRequest): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Confirming upload for organization $organizationId, job: ${request.jobId}" }
        return pipeline.send(
            ConfirmUploadCommand(
                organizationId = organizationId,
                jobId = request.jobId
            )
        )
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "Get presigned URLs for batch upload", description = "Generate presigned URLs for uploading multiple files")
    fun getBatchUploadUrls(@RequestBody request: BatchUploadRequest): BatchUploadResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Generating batch presigned URLs for organization $organizationId, ${request.files.size} files" }
        return pipeline.send(
            BatchUploadCommand(
                organizationId = organizationId,
                files = request.files,
                knowledgeBaseId = request.knowledgeBaseId
            )
        )
    }

    @PostMapping("/upload/batch/confirm")
    @Operation(summary = "Confirm batch upload completion", description = "Confirm that all files in the batch are uploaded and start processing")
    fun confirmBatchUpload(@RequestBody request: ConfirmUploadRequest): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Confirming batch upload for organization $organizationId, batch: ${request.jobId}" }
        return pipeline.send(
            ConfirmBatchUploadCommand(
                organizationId = organizationId,
                batchJobId = request.jobId
            )
        )
    }

    @PostMapping("/upload/folder")
    @Operation(summary = "Get presigned URL for folder/ZIP upload", description = "Generate a presigned URL for uploading a ZIP archive containing a folder structure")
    fun getFolderUploadUrl(@RequestBody request: FolderUploadRequest): PresignedUploadResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Generating folder upload URL for organization $organizationId, file: ${request.fileName}" }
        return pipeline.send(
            FolderUploadCommand(
                organizationId = organizationId,
                fileName = request.fileName,
                fileSize = request.fileSize,
                knowledgeBaseId = request.knowledgeBaseId,
                preserveStructure = request.preserveStructure
            )
        )
    }

    @PostMapping("/scrape")
    @Operation(summary = "Submit web scrape job", description = "Submit a URL for web scraping")
    fun submitScrapeJob(@RequestBody request: WebScrapeRequest): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Submitting scrape job for organization $organizationId, url: ${request.url}" }
        return pipeline.send(
            SubmitScrapeJobCommand(
                organizationId = organizationId,
                url = request.url,
                knowledgeBaseId = request.knowledgeBaseId,
                depth = request.maxDepth,
                maxPages = request.maxPages
            )
        )
    }

    @PostMapping("/text")
    @Operation(summary = "Submit text input", description = "Submit raw text content for ingestion")
    fun submitTextInput(@RequestBody request: TextInputRequest): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Submitting text input for organization $organizationId, title: ${request.title}" }
        return pipeline.send(
            SubmitTextInputCommand(
                organizationId = organizationId,
                content = request.content,
                title = request.title,
                knowledgeBaseId = request.knowledgeBaseId,
                metadata = request.metadata
            )
        )
    }

    @PostMapping("/qa-pairs")
    @Operation(summary = "Submit Q&A pairs", description = "Submit question-answer pairs for FAQ-style ingestion")
    fun submitQAPairs(@RequestBody request: QAPairsRequest): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Submitting Q&A pairs for organization $organizationId, ${request.pairs.size} pairs" }
        return pipeline.send(
            SubmitQAPairsCommand(
                organizationId = organizationId,
                pairs = request.pairs,
                knowledgeBaseId = request.knowledgeBaseId,
                sourceName = request.sourceName
            )
        )
    }

    @PostMapping("/rss")
    @Operation(summary = "Submit RSS/Atom feed", description = "Subscribe to an RSS or Atom feed for ingestion")
    fun submitRssFeed(@RequestBody request: RssFeedRequest): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        log.info { "Submitting RSS feed for organization $organizationId, feed: ${request.feedUrl}" }
        return pipeline.send(
            SubmitRssFeedCommand(
                organizationId = organizationId,
                feedUrl = request.feedUrl,
                knowledgeBaseId = request.knowledgeBaseId,
                sourceName = request.sourceName,
                maxItems = request.maxItems,
                includeFullContent = request.includeFullContent
            )
        )
    }

    @GetMapping("/jobs")
    @Operation(summary = "List ingestion jobs", description = "List all ingestion jobs for the current organization")
    fun listJobs(
        @RequestParam(required = false) status: JobStatus?,
        @RequestParam(required = false) knowledgeBaseId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): JobListResponse {
        val organizationId = getCurrentOrganizationId()
        return pipeline.send(
            ListJobsQuery(
                organizationId = organizationId,
                status = status,
                knowledgeBaseId = knowledgeBaseId,
                page = page,
                size = size
            )
        )
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Get the status of a specific ingestion job")
    fun getJob(@PathVariable jobId: UUID): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        return pipeline.send(
            GetJobQuery(
                organizationId = organizationId,
                jobId = jobId
            )
        )
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Cancel job", description = "Cancel a pending or queued ingestion job")
    fun cancelJob(@PathVariable jobId: UUID): Map<String, Any> {
        val organizationId = getCurrentOrganizationId()
        val result = pipeline.send(
            CancelJobCommand(
                organizationId = organizationId,
                jobId = jobId
            )
        )
        return mapOf("success" to result.success, "message" to result.message)
    }

    @PostMapping("/jobs/{jobId}/retry")
    @Operation(summary = "Retry failed job", description = "Retry a failed ingestion job")
    fun retryJob(@PathVariable jobId: UUID): IngestionJobResponse {
        val organizationId = getCurrentOrganizationId()
        return pipeline.send(
            RetryJobCommand(
                organizationId = organizationId,
                jobId = jobId
            )
        )
    }

    @GetMapping("/quota")
    @Operation(summary = "Get organization quota", description = "Get quota and usage information for the current organization")
    fun getOrganizationQuota(): OrganizationQuotaResponse {
        val organizationId = getCurrentOrganizationId()
        return pipeline.send(
            GetOrganizationQuotaQuery(organizationId = organizationId)
        )
    }

    private fun getCurrentOrganizationId(): UUID = securityContextService.getCurrentMerchantId()
}
