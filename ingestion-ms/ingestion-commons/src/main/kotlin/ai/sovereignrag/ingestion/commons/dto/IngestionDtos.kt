package ai.sovereignrag.ingestion.commons.dto

import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Request for presigned upload URL")
data class PresignedUploadRequest(
    @Schema(description = "Original file name", example = "document.pdf")
    val fileName: String,
    @Schema(description = "File MIME type", example = "application/pdf")
    val contentType: String,
    @Schema(description = "File size in bytes", example = "1048576")
    val fileSize: Long,
    @Schema(description = "Knowledge base ID to associate with", example = "123e4567-e89b-12d3-a456-426614174000")
    val knowledgeBaseId: UUID? = null
)

@Schema(description = "Response with presigned upload URL")
data class PresignedUploadResponse(
    @Schema(description = "Job ID for tracking")
    val jobId: UUID,
    @Schema(description = "Presigned URL for direct upload to S3")
    val uploadUrl: String,
    @Schema(description = "S3 key where file will be stored")
    val key: String,
    @Schema(description = "URL expiration time in seconds")
    val expiresIn: Long
)

@Schema(description = "Request to confirm upload completion")
data class ConfirmUploadRequest(
    @Schema(description = "Job ID from presigned upload response")
    val jobId: UUID
)

@Schema(description = "Request to submit web scrape job")
data class WebScrapeRequest(
    @Schema(description = "URL to scrape", example = "https://example.com/docs")
    val url: String,
    @Schema(description = "Knowledge base ID to associate with")
    val knowledgeBaseId: UUID? = null,
    @Schema(description = "Whether to follow links (crawl)")
    val crawl: Boolean = false,
    @Schema(description = "Maximum depth for crawling")
    val maxDepth: Int = 1,
    @Schema(description = "Maximum pages to scrape")
    val maxPages: Int = 10
)

@Schema(description = "Request for batch import")
data class BatchImportRequest(
    @Schema(description = "List of URLs to import")
    val urls: List<String>? = null,
    @Schema(description = "S3 key of ZIP file to import")
    val zipFileKey: String? = null,
    @Schema(description = "Knowledge base ID to associate with")
    val knowledgeBaseId: UUID? = null
)

@Schema(description = "Job response")
data class IngestionJobResponse(
    @Schema(description = "Job ID")
    val id: UUID,
    @Schema(description = "Organization ID")
    val organizationId: UUID,
    @Schema(description = "Knowledge base ID")
    val knowledgeBaseId: UUID?,
    @Schema(description = "Job type")
    val jobType: JobType,
    @Schema(description = "Current status")
    val status: JobStatus,
    @Schema(description = "Source type")
    val sourceType: SourceType?,
    @Schema(description = "File name or URL")
    val fileName: String?,
    @Schema(description = "File size in bytes")
    val fileSize: Long?,
    @Schema(description = "MIME type")
    val mimeType: String?,
    @Schema(description = "Progress percentage (0-100)")
    val progress: Int,
    @Schema(description = "Error message if failed")
    val errorMessage: String?,
    @Schema(description = "Number of retries attempted")
    val retryCount: Int,
    @Schema(description = "Number of chunks created")
    val chunksCreated: Int,
    @Schema(description = "Bytes processed")
    val bytesProcessed: Long,
    @Schema(description = "Job creation time")
    val createdAt: Instant,
    @Schema(description = "Processing start time")
    val startedAt: Instant?,
    @Schema(description = "Completion time")
    val completedAt: Instant?,
    @Schema(description = "Processing duration in milliseconds")
    val processingDurationMs: Long?
)

@Schema(description = "Job item response for batch jobs")
data class IngestionJobItemResponse(
    @Schema(description = "Item ID")
    val id: UUID,
    @Schema(description = "Item index in batch")
    val itemIndex: Int,
    @Schema(description = "Source reference")
    val sourceReference: String?,
    @Schema(description = "File name")
    val fileName: String?,
    @Schema(description = "File size")
    val fileSize: Long?,
    @Schema(description = "MIME type")
    val mimeType: String?,
    @Schema(description = "Item status")
    val status: JobStatus,
    @Schema(description = "Error message if failed")
    val errorMessage: String?,
    @Schema(description = "Chunks created")
    val chunksCreated: Int,
    @Schema(description = "Bytes processed")
    val bytesProcessed: Long
)

@Schema(description = "Paginated job list response")
data class JobListResponse(
    @Schema(description = "List of jobs")
    val jobs: List<IngestionJobResponse>,
    @Schema(description = "Total number of jobs")
    val total: Long,
    @Schema(description = "Current page number")
    val page: Int,
    @Schema(description = "Page size")
    val pageSize: Int,
    @Schema(description = "Total pages")
    val totalPages: Int
)

@Schema(description = "Organization quota information")
data class OrganizationQuotaResponse(
    @Schema(description = "Organization ID")
    val organizationId: UUID,
    @Schema(description = "Maximum file size in bytes")
    val maxFileSize: Long,
    @Schema(description = "Maximum concurrent jobs")
    val maxConcurrentJobs: Int,
    @Schema(description = "Current active jobs count")
    val activeJobsCount: Long,
    @Schema(description = "Total bytes processed")
    val totalBytesProcessed: Long,
    @Schema(description = "Storage quota in bytes")
    val storageQuota: Long?,
    @Schema(description = "Storage used in bytes")
    val storageUsed: Long?
)
