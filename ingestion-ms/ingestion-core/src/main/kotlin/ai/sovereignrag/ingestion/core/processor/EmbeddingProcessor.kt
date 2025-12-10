package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.chunking.ChunkMetadata as ChunkingMetadata
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.embedding.ChunkMetadata
import ai.sovereignrag.commons.embedding.EmbeddingGateway
import ai.sovereignrag.commons.embedding.EmbeddingModelGateway
import ai.sovereignrag.commons.embedding.EmbeddingModelNotFoundException
import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.embedding.TextChunk
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.MetricSource
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.embedding.EmbeddingService
import ai.sovereignrag.ingestion.core.metrics.AsyncQualityEvaluationService
import ai.sovereignrag.ingestion.core.metrics.IngestionMetrics
import ai.sovereignrag.ingestion.core.metrics.QualityEvaluationRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class EmbeddingProcessor(
    private val embeddingService: EmbeddingService,
    private val embeddingGateway: EmbeddingGateway,
    private val embeddingModelGateway: EmbeddingModelGateway,
    private val jobRepository: IngestionJobRepository,
    private val objectMapper: ObjectMapper,
    private val asyncQualityService: AsyncQualityEvaluationService,
    private val ingestionMetrics: IngestionMetrics
) : JobProcessor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.EMBEDDING

    override fun process(job: IngestionJob) {
        log.info { "Processing embedding job ${job.id} for knowledge base ${job.knowledgeBaseId}" }

        val knowledgeBaseId = job.knowledgeBaseId
            ?: throw IllegalStateException("Knowledge base ID is required for embedding job ${job.id}")

        val knowledgeSourceId = job.knowledgeSourceId
            ?: throw IllegalStateException("Knowledge source ID is required for embedding job ${job.id}")

        updateProgress(job, 10)

        val modelConfig = embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId)
            ?: throw EmbeddingModelNotFoundException("No embedding model configured for knowledge base $knowledgeBaseId")

        log.info { "Using embedding model: ${modelConfig.name} (${modelConfig.provider}/${modelConfig.modelId})" }

        val chunkData = parseChunkData(job)
        val chunks = chunkData.chunks

        log.info {
            "Embedding ${chunks.size} chunks for source $knowledgeSourceId " +
            "(strategy: ${chunkData.chunkingStrategy ?: "unknown"}, quality: ${chunkData.qualityScore?.let { "%.2f".format(it) } ?: "N/A"})"
        }

        updateProgress(job, 20)

        val embeddingStartTime = System.currentTimeMillis()
        val texts = chunks.map { it.content }
        val embeddings = embeddingService.generateEmbeddings(texts, modelConfig)
        val embeddingTimeMs = System.currentTimeMillis() - embeddingStartTime

        updateProgress(job, 60)

        val textChunks = chunks.mapIndexed { index, chunk ->
            TextChunk(
                id = UUID.randomUUID(),
                content = chunk.content,
                embedding = embeddings[index],
                chunkIndex = chunk.index,
                metadata = ChunkMetadata(
                    sourceId = knowledgeSourceId,
                    sourceType = mapSourceType(chunkData.sourceType),
                    fileName = chunkData.fileName,
                    sourceUrl = chunkData.sourceUrl,
                    title = chunkData.title,
                    totalChunks = chunks.size,
                    createdAt = Instant.now()
                )
            )
        }

        updateProgress(job, 80)

        val embeddingIds = embeddingGateway.storeEmbeddings(
            knowledgeBaseId = knowledgeBaseId.toString(),
            sourceId = knowledgeSourceId,
            chunks = textChunks
        )

        updateProgress(job, 90)

        ingestionMetrics.recordEmbeddingMetrics(
            knowledgeBaseId = knowledgeBaseId.toString(),
            embeddingCount = embeddings.size,
            processingTimeMs = embeddingTimeMs,
            model = modelConfig.modelId
        )

        triggerAsyncQualityEvaluation(
            job = job,
            chunks = chunks,
            embeddings = embeddings,
            chunkData = chunkData,
            modelName = modelConfig.modelId
        )

        job.embeddingsCreated = embeddingIds.size
        job.markCompleted(chunksCreated = chunks.size, bytesProcessed = texts.sumOf { it.length.toLong() })
        jobRepository.save(job)

        log.info { "Completed embedding job ${job.id}: created ${embeddingIds.size} embeddings" }
    }

    private fun triggerAsyncQualityEvaluation(
        job: IngestionJob,
        chunks: List<ChunkInfo>,
        embeddings: List<FloatArray>,
        chunkData: ChunkJobData,
        modelName: String
    ) {
        val documentChunks = chunks.mapIndexed { index, chunk ->
            DocumentChunk(
                content = chunk.content,
                index = index,
                startOffset = 0,
                endOffset = chunk.content.length,
                metadata = ChunkingMetadata(
                    strategyUsed = chunkData.chunkingStrategy
                )
            )
        }

        val request = QualityEvaluationRequest(
            organizationId = job.organizationId,
            knowledgeBaseId = job.knowledgeBaseId,
            knowledgeSourceId = job.knowledgeSourceId,
            ingestionJobId = job.id,
            chunks = documentChunks,
            embeddings = embeddings,
            chunkingStrategy = chunkData.chunkingStrategy,
            embeddingModel = modelName,
            metricSource = MetricSource.EMBEDDING
        )

        asyncQualityService.evaluateChunkQualityAsync(request)
            .whenComplete { result, error ->
                error?.let {
                    log.warn(it) { "Async quality evaluation failed for job ${job.id}" }
                } ?: result?.let {
                    log.debug { "Quality evaluation completed for job ${job.id}: score=${it.overallScore}" }
                }
            }
    }

    private fun parseChunkData(job: IngestionJob): ChunkJobData {
        val metadata = job.metadata
            ?: throw IllegalStateException("Metadata is required for embedding job ${job.id}")

        return objectMapper.readValue<ChunkJobData>(metadata)
    }

    private fun mapSourceType(sourceType: String): SourceType {
        return SourceType.entries.find { it.name.equals(sourceType, ignoreCase = true) }
            ?: SourceType.FILE
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}

data class ChunkJobData(
    val chunks: List<ChunkInfo>,
    val sourceType: String,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val chunkingStrategy: String? = null,
    val qualityScore: Double? = null
)

data class ChunkInfo(
    val index: Int,
    val content: String
)
