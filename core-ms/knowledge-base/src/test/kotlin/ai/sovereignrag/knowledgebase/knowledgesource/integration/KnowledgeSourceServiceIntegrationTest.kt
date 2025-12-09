package ai.sovereignrag.knowledgebase.knowledgesource.integration

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.commons.knowledgesource.UpdateKnowledgeSourceRequest
import ai.sovereignrag.knowledgebase.knowledgesource.repository.KnowledgeSourceRepository
import ai.sovereignrag.knowledgebase.knowledgesource.service.KnowledgeSourceService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class KnowledgeSourceServiceIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("kb_test")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @Autowired
    private lateinit var knowledgeSourceService: KnowledgeSourceService

    @Autowired
    private lateinit var knowledgeSourceRepository: KnowledgeSourceRepository

    private val knowledgeBaseId = UUID.randomUUID()
    private val createdSourceIds = mutableListOf<UUID>()

    @AfterEach
    fun cleanup() {
        createdSourceIds.forEach { id ->
            knowledgeSourceRepository.deleteById(id)
        }
        createdSourceIds.clear()
    }

    @Test
    fun `create should persist file knowledge source`() {
        val request = createFileSourceRequest(
            fileName = "test-doc.pdf",
            title = "Test Document",
            mimeType = "application/pdf",
            fileSize = 2048L,
            s3Key = "uploads/test-doc.pdf"
        )

        val result = knowledgeSourceService.create(knowledgeBaseId, request)
        createdSourceIds.add(result.id)

        assertNotNull(result.id)
        assertEquals(knowledgeBaseId, result.knowledgeBaseId)
        assertEquals(SourceType.FILE, result.sourceType)
        assertEquals("test-doc.pdf", result.fileName)
        assertEquals("Test Document", result.title)
        assertEquals("application/pdf", result.mimeType)
        assertEquals(2048L, result.fileSize)
        assertEquals(KnowledgeSourceStatus.PENDING, result.status)
    }

    @Test
    fun `create should use fileName as title when title not provided`() {
        val request = createFileSourceRequest(
            fileName = "auto-title.pdf",
            title = null,
            mimeType = "application/pdf",
            fileSize = 1024L,
            s3Key = "uploads/auto-title.pdf"
        )

        val result = knowledgeSourceService.create(knowledgeBaseId, request)
        createdSourceIds.add(result.id)

        assertEquals("auto-title.pdf", result.title)
    }

    @Test
    fun `create should persist URL knowledge source`() {
        val request = createUrlSourceRequest(
            sourceUrl = "https://example.com/page",
            title = "Example Page"
        )

        val result = knowledgeSourceService.create(knowledgeBaseId, request)
        createdSourceIds.add(result.id)

        assertEquals(SourceType.URL, result.sourceType)
        assertEquals("https://example.com/page", result.sourceUrl)
        assertEquals("Example Page", result.title)
    }

    @Test
    fun `create should use sourceUrl as title when title not provided for URL source`() {
        val request = createUrlSourceRequest(
            sourceUrl = "https://example.com/docs",
            title = null
        )

        val result = knowledgeSourceService.create(knowledgeBaseId, request)
        createdSourceIds.add(result.id)

        assertEquals("https://example.com/docs", result.title)
    }

    @Test
    fun `update should modify title and metadata`() {
        val createRequest = createFileSourceRequest(
            fileName = "original.pdf",
            title = "Original Title"
        )
        val created = knowledgeSourceService.create(knowledgeBaseId, createRequest)
        createdSourceIds.add(created.id)

        val updateRequest = UpdateKnowledgeSourceRequest(
            title = "Updated Title",
            metadata = mapOf("key" to "value")
        )

        val updated = knowledgeSourceService.update(knowledgeBaseId, created.id, updateRequest)

        assertEquals("Updated Title", updated.title)
        assertEquals(mapOf("key" to "value"), updated.metadata)
    }

    @Test
    fun `updateStatus should change status and set error message`() {
        val createRequest = createFileSourceRequest(fileName = "failing.pdf")
        val created = knowledgeSourceService.create(knowledgeBaseId, createRequest)
        createdSourceIds.add(created.id)

        knowledgeSourceService.updateStatus(
            knowledgeBaseId,
            created.id,
            KnowledgeSourceStatus.FAILED,
            "Processing failed"
        )

        val retrieved = knowledgeSourceService.findById(knowledgeBaseId, created.id)
        assertNotNull(retrieved)
        assertEquals(KnowledgeSourceStatus.FAILED, retrieved.status)
        assertEquals("Processing failed", retrieved.errorMessage)
    }

    @Test
    fun `updateEmbeddingStats should update chunk and embedding counts`() {
        val createRequest = createFileSourceRequest(fileName = "embedded.pdf")
        val created = knowledgeSourceService.create(knowledgeBaseId, createRequest)
        createdSourceIds.add(created.id)

        knowledgeSourceService.updateEmbeddingStats(
            knowledgeBaseId,
            created.id,
            chunkCount = 25,
            embeddingCount = 25
        )

        val retrieved = knowledgeSourceService.findById(knowledgeBaseId, created.id)
        assertNotNull(retrieved)
        assertEquals(25, retrieved.chunkCount)
        assertEquals(25, retrieved.embeddingCount)
    }

    @Test
    fun `findByKnowledgeBase should return paginated results`() {
        repeat(5) { i ->
            val request = createFileSourceRequest(fileName = "doc-$i.pdf")
            val created = knowledgeSourceService.create(knowledgeBaseId, request)
            createdSourceIds.add(created.id)
        }

        val page1 = knowledgeSourceService.findByKnowledgeBase(knowledgeBaseId, page = 0, size = 2)
        val page2 = knowledgeSourceService.findByKnowledgeBase(knowledgeBaseId, page = 1, size = 2)

        assertEquals(2, page1.content.size)
        assertEquals(5, page1.totalElements)
        assertEquals(3, page1.totalPages)
        assertEquals(2, page2.content.size)
    }

    @Test
    fun `findByStatus should filter by status`() {
        val pending = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "pending.pdf"))
        createdSourceIds.add(pending.id)

        val ready = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "ready.pdf"))
        createdSourceIds.add(ready.id)
        knowledgeSourceService.updateStatus(knowledgeBaseId, ready.id, KnowledgeSourceStatus.READY, null)

        val pendingSources = knowledgeSourceService.findByStatus(knowledgeBaseId, KnowledgeSourceStatus.PENDING)
        val readySources = knowledgeSourceService.findByStatus(knowledgeBaseId, KnowledgeSourceStatus.READY)

        assertEquals(1, pendingSources.size)
        assertEquals(1, readySources.size)
        assertEquals("pending.pdf", pendingSources[0].fileName)
        assertEquals("ready.pdf", readySources[0].fileName)
    }

    @Test
    fun `delete should soft delete and exclude from queries`() {
        val request = createFileSourceRequest(fileName = "to-delete.pdf")
        val created = knowledgeSourceService.create(knowledgeBaseId, request)
        createdSourceIds.add(created.id)

        knowledgeSourceService.delete(knowledgeBaseId, created.id)

        val retrieved = knowledgeSourceService.findById(knowledgeBaseId, created.id)
        assertNull(retrieved)

        val count = knowledgeSourceService.countByKnowledgeBase(knowledgeBaseId)
        assertEquals(0, count)
    }

    @Test
    fun `countByKnowledgeBase should exclude deleted sources`() {
        repeat(3) { i ->
            val request = createFileSourceRequest(fileName = "doc-$i.pdf")
            val created = knowledgeSourceService.create(knowledgeBaseId, request)
            createdSourceIds.add(created.id)
        }

        val initialCount = knowledgeSourceService.countByKnowledgeBase(knowledgeBaseId)
        assertEquals(3, initialCount)

        knowledgeSourceService.delete(knowledgeBaseId, createdSourceIds[0])

        val afterDeleteCount = knowledgeSourceService.countByKnowledgeBase(knowledgeBaseId)
        assertEquals(2, afterDeleteCount)
    }

    @Test
    fun `countByStatus should count sources with specific status`() {
        repeat(2) { i ->
            val created = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "pending-$i.pdf"))
            createdSourceIds.add(created.id)
        }

        val ready = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "ready.pdf"))
        createdSourceIds.add(ready.id)
        knowledgeSourceService.updateStatus(knowledgeBaseId, ready.id, KnowledgeSourceStatus.READY, null)

        val pendingCount = knowledgeSourceService.countByStatus(knowledgeBaseId, KnowledgeSourceStatus.PENDING)
        val readyCount = knowledgeSourceService.countByStatus(knowledgeBaseId, KnowledgeSourceStatus.READY)

        assertEquals(2, pendingCount)
        assertEquals(1, readyCount)
    }

    @Test
    fun `deleteByKnowledgeBase should soft delete all sources for KB`() {
        repeat(3) { i ->
            val created = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "doc-$i.pdf"))
            createdSourceIds.add(created.id)
        }

        knowledgeSourceService.deleteByKnowledgeBase(knowledgeBaseId)

        val count = knowledgeSourceService.countByKnowledgeBase(knowledgeBaseId)
        assertEquals(0, count)
    }

    @Test
    fun `markReady should set status and embedding count`() {
        val created = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "complete.pdf"))
        createdSourceIds.add(created.id)

        knowledgeSourceService.markReady(knowledgeBaseId, created.id, embeddingCount = 50)

        val retrieved = knowledgeSourceService.findById(knowledgeBaseId, created.id)
        assertNotNull(retrieved)
        assertEquals(KnowledgeSourceStatus.READY, retrieved.status)
        assertEquals(50, retrieved.embeddingCount)
        assertNotNull(retrieved.processedAt)
    }

    @Test
    fun `getTotalEmbeddingCount should sum embeddings for knowledge base`() {
        repeat(3) { i ->
            val created = knowledgeSourceService.create(knowledgeBaseId, createFileSourceRequest(fileName = "doc-$i.pdf"))
            createdSourceIds.add(created.id)
            knowledgeSourceService.updateEmbeddingStats(knowledgeBaseId, created.id, 10 * (i + 1), 10 * (i + 1))
        }

        val total = knowledgeSourceService.getTotalEmbeddingCount(knowledgeBaseId)

        assertEquals(60, total)
    }

    private fun createFileSourceRequest(
        fileName: String,
        title: String? = fileName,
        mimeType: String = "application/pdf",
        fileSize: Long = 1024L,
        s3Key: String = "uploads/$fileName"
    ): CreateKnowledgeSourceRequest {
        return CreateKnowledgeSourceRequest(
            sourceType = SourceType.FILE,
            fileName = fileName,
            sourceUrl = null,
            title = title,
            mimeType = mimeType,
            fileSize = fileSize,
            s3Key = s3Key,
            ingestionJobId = null,
            metadata = emptyMap()
        )
    }

    private fun createUrlSourceRequest(
        sourceUrl: String,
        title: String? = sourceUrl
    ): CreateKnowledgeSourceRequest {
        return CreateKnowledgeSourceRequest(
            sourceType = SourceType.URL,
            fileName = null,
            sourceUrl = sourceUrl,
            title = title,
            mimeType = "text/html",
            fileSize = null,
            s3Key = null,
            ingestionJobId = null,
            metadata = emptyMap()
        )
    }
}
