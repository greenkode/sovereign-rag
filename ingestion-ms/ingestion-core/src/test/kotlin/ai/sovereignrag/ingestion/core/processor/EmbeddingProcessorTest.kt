package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.embedding.EmbeddingGateway
import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.commons.embedding.EmbeddingModelGateway
import ai.sovereignrag.commons.embedding.EmbeddingModelNotFoundException
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.embedding.EmbeddingService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EmbeddingProcessorTest {

    private val embeddingService: EmbeddingService = mockk()
    private val embeddingGateway: EmbeddingGateway = mockk()
    private val embeddingModelGateway: EmbeddingModelGateway = mockk()
    private val jobRepository: IngestionJobRepository = mockk()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var processor: EmbeddingProcessor

    private val organizationId = UUID.randomUUID()
    private val knowledgeBaseId = UUID.randomUUID()
    private val knowledgeSourceId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        processor = EmbeddingProcessor(
            embeddingService,
            embeddingGateway,
            embeddingModelGateway,
            jobRepository,
            objectMapper
        )
    }

    @Test
    fun `process should throw when knowledge base ID is null`() {
        val job = createMockJob(knowledgeBaseId = null)

        assertThrows<IllegalStateException> {
            processor.process(job)
        }
    }

    @Test
    fun `process should throw when knowledge source ID is null`() {
        val job = createMockJob(knowledgeSourceId = null)

        assertThrows<IllegalStateException> {
            processor.process(job)
        }
    }

    @Test
    fun `process should throw when no embedding model configured for KB`() {
        val job = createMockJob()

        every { jobRepository.save(any()) } returnsArgument 0
        every { embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId) } returns null

        assertThrows<EmbeddingModelNotFoundException> {
            processor.process(job)
        }
    }

    @Test
    fun `process should generate embeddings and store them`() {
        val job = createMockJob()
        val modelConfig = createMockModelConfig()
        val chunkData = ChunkJobData(
            chunks = listOf(
                ChunkInfo(0, "First chunk content"),
                ChunkInfo(1, "Second chunk content")
            ),
            sourceType = "FILE",
            fileName = "test.pdf",
            sourceUrl = null,
            title = "Test Document"
        )
        job.metadata = objectMapper.writeValueAsString(chunkData)

        val embeddings = listOf(
            FloatArray(1536) { 0.1f },
            FloatArray(1536) { 0.2f }
        )
        val embeddingIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        every { jobRepository.save(any()) } returnsArgument 0
        every { embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId) } returns modelConfig
        every { embeddingService.generateEmbeddings(any(), modelConfig) } returns embeddings
        every { embeddingGateway.storeEmbeddings(any(), any(), any()) } returns embeddingIds

        processor.process(job)

        verify { embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId) }
        verify { embeddingService.generateEmbeddings(listOf("First chunk content", "Second chunk content"), modelConfig) }
        verify { embeddingGateway.storeEmbeddings(knowledgeBaseId.toString(), knowledgeSourceId, any()) }
        assertEquals(JobStatus.COMPLETED, job.status)
        assertEquals(2, job.embeddingsCreated)
        assertEquals(100, job.progress)
    }

    @Test
    fun `process should update progress throughout processing`() {
        val job = createMockJob()
        val modelConfig = createMockModelConfig()
        val chunkData = ChunkJobData(
            chunks = listOf(ChunkInfo(0, "Content")),
            sourceType = "FILE",
            fileName = "test.pdf",
            sourceUrl = null,
            title = "Test"
        )
        job.metadata = objectMapper.writeValueAsString(chunkData)

        val progressCaptures = mutableListOf<Int>()
        val jobSlot = slot<IngestionJob>()

        every { jobRepository.save(capture(jobSlot)) } answers {
            progressCaptures.add(jobSlot.captured.progress)
            jobSlot.captured
        }
        every { embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId) } returns modelConfig
        every { embeddingService.generateEmbeddings(any(), modelConfig) } returns listOf(FloatArray(1536))
        every { embeddingGateway.storeEmbeddings(any(), any(), any()) } returns listOf(UUID.randomUUID())

        processor.process(job)

        assert(progressCaptures.contains(10))
        assert(progressCaptures.contains(20))
        assert(progressCaptures.contains(60))
        assert(progressCaptures.contains(80))
        assert(progressCaptures.contains(90))
    }

    @Test
    fun `process should use correct embedding model from gateway`() {
        val job = createMockJob()
        val modelConfig = createMockModelConfig(provider = "openai", modelId = "text-embedding-3-large")
        val chunkData = ChunkJobData(
            chunks = listOf(ChunkInfo(0, "Content")),
            sourceType = "FILE",
            fileName = "test.pdf",
            sourceUrl = null,
            title = "Test"
        )
        job.metadata = objectMapper.writeValueAsString(chunkData)

        val modelConfigSlot = slot<EmbeddingModelConfig>()

        every { jobRepository.save(any()) } returnsArgument 0
        every { embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId) } returns modelConfig
        every { embeddingService.generateEmbeddings(any(), capture(modelConfigSlot)) } returns listOf(FloatArray(3072))
        every { embeddingGateway.storeEmbeddings(any(), any(), any()) } returns listOf(UUID.randomUUID())

        processor.process(job)

        assertEquals("openai", modelConfigSlot.captured.provider)
        assertEquals("text-embedding-3-large", modelConfigSlot.captured.modelId)
    }

    @Test
    fun `process should throw when metadata is null`() {
        val job = createMockJob()
        job.metadata = null
        val modelConfig = createMockModelConfig()

        every { jobRepository.save(any()) } returnsArgument 0
        every { embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId) } returns modelConfig

        assertThrows<IllegalStateException> {
            processor.process(job)
        }
    }

    private fun createMockJob(
        knowledgeBaseId: UUID? = this.knowledgeBaseId,
        knowledgeSourceId: UUID? = this.knowledgeSourceId
    ): IngestionJob {
        return IngestionJob(
            organizationId = organizationId,
            jobType = JobType.EMBEDDING,
            knowledgeBaseId = knowledgeBaseId
        ).apply {
            id = UUID.randomUUID()
            this.knowledgeSourceId = knowledgeSourceId
            status = JobStatus.PROCESSING
        }
    }

    private fun createMockModelConfig(
        id: String = "openai-text-3-small",
        provider: String = "openai",
        modelId: String = "text-embedding-3-small"
    ): EmbeddingModelConfig {
        return object : EmbeddingModelConfig {
            override val id: String = id
            override val name: String = "Test Model"
            override val modelId: String = modelId
            override val provider: String = provider
            override val dimensions: Int = 1536
            override val maxTokens: Int = 8191
            override val baseUrl: String? = null
        }
    }
}
