package ai.sovereignrag.knowledgebase.knowledgesource.unit

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.knowledgebase.knowledgesource.domain.KnowledgeSource
import ai.sovereignrag.knowledgebase.knowledgesource.query.GetKnowledgeSourceQuery
import ai.sovereignrag.knowledgebase.knowledgesource.query.GetKnowledgeSourceQueryHandler
import ai.sovereignrag.knowledgebase.knowledgesource.repository.KnowledgeSourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GetKnowledgeSourceQueryHandlerTest {

    private val knowledgeSourceRepository: KnowledgeSourceRepository = mockk()

    private lateinit var handler: GetKnowledgeSourceQueryHandler

    private val knowledgeBaseId = UUID.randomUUID()
    private val sourceId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = GetKnowledgeSourceQueryHandler(knowledgeSourceRepository)
    }

    @Test
    fun `should return null when knowledge source does not exist`() {
        val query = GetKnowledgeSourceQuery(
            knowledgeBaseId = knowledgeBaseId,
            sourceId = sourceId
        )

        every {
            knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
                id = sourceId,
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED
            )
        } returns null

        val result = handler.handle(query)

        assertNull(result)
        verify {
            knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
                id = sourceId,
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED
            )
        }
    }

    @Test
    fun `should return knowledge source dto when found`() {
        val query = GetKnowledgeSourceQuery(
            knowledgeBaseId = knowledgeBaseId,
            sourceId = sourceId
        )
        val source = createMockKnowledgeSource()

        every {
            knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
                id = sourceId,
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED
            )
        } returns source

        val result = handler.handle(query)

        assertNotNull(result)
        assertEquals(sourceId, result.id)
        assertEquals(knowledgeBaseId, result.knowledgeBaseId)
        assertEquals(SourceType.FILE, result.sourceType)
        assertEquals("test-file.pdf", result.fileName)
        assertEquals("Test Document", result.title)
        assertEquals("application/pdf", result.mimeType)
        assertEquals(1024L, result.fileSize)
        assertEquals(KnowledgeSourceStatus.READY, result.status)
        assertEquals(10, result.chunkCount)
        assertEquals(10, result.embeddingCount)
    }

    @Test
    fun `should return null when source is deleted`() {
        val query = GetKnowledgeSourceQuery(
            knowledgeBaseId = knowledgeBaseId,
            sourceId = sourceId
        )

        every {
            knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
                id = sourceId,
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED
            )
        } returns null

        val result = handler.handle(query)

        assertNull(result)
    }

    @Test
    fun `should map all fields correctly to dto`() {
        val processedAt = Instant.now().minusSeconds(3600)
        val source = createMockKnowledgeSource().copy(
            sourceUrl = "https://example.com/doc.pdf",
            errorMessage = null,
            processedAt = processedAt
        )

        every {
            knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
                id = sourceId,
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED
            )
        } returns source

        val result = handler.handle(
            GetKnowledgeSourceQuery(knowledgeBaseId = knowledgeBaseId, sourceId = sourceId)
        )

        assertNotNull(result)
        assertEquals("https://example.com/doc.pdf", result.sourceUrl)
        assertEquals(processedAt, result.processedAt)
        assertNull(result.errorMessage)
    }

    @Test
    fun `should include error message when source has failed status`() {
        val source = createMockKnowledgeSource().copy(
            status = KnowledgeSourceStatus.FAILED,
            errorMessage = "Failed to process document"
        )

        every {
            knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
                id = sourceId,
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED
            )
        } returns source

        val result = handler.handle(
            GetKnowledgeSourceQuery(knowledgeBaseId = knowledgeBaseId, sourceId = sourceId)
        )

        assertNotNull(result)
        assertEquals(KnowledgeSourceStatus.FAILED, result.status)
        assertEquals("Failed to process document", result.errorMessage)
    }

    private fun createMockKnowledgeSource(): KnowledgeSource {
        return KnowledgeSource(
            id = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            sourceType = SourceType.FILE,
            fileName = "test-file.pdf",
            title = "Test Document",
            mimeType = "application/pdf",
            fileSize = 1024L,
            status = KnowledgeSourceStatus.READY,
            chunkCount = 10,
            embeddingCount = 10,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
