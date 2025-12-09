package ai.sovereignrag.knowledgebase.knowledgesource.unit

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.knowledgebase.knowledgesource.domain.KnowledgeSource
import ai.sovereignrag.knowledgebase.knowledgesource.query.GetKnowledgeSourcesQuery
import ai.sovereignrag.knowledgebase.knowledgesource.query.GetKnowledgeSourcesQueryHandler
import ai.sovereignrag.knowledgebase.knowledgesource.repository.KnowledgeSourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetKnowledgeSourcesQueryHandlerTest {

    private val knowledgeSourceRepository: KnowledgeSourceRepository = mockk()

    private lateinit var handler: GetKnowledgeSourcesQueryHandler

    private val knowledgeBaseId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = GetKnowledgeSourcesQueryHandler(knowledgeSourceRepository)
    }

    @Test
    fun `should return empty page when no knowledge sources exist`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
        val query = GetKnowledgeSourcesQuery(knowledgeBaseId = knowledgeBaseId, pageable = pageable)

        every {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = pageable
            )
        } returns PageImpl(emptyList())

        val result = handler.handle(query)

        assertTrue(result.isEmpty)
        assertEquals(0, result.totalElements)
    }

    @Test
    fun `should return paginated knowledge sources`() {
        val pageable = PageRequest.of(0, 2)
        val sources = listOf(
            createMockKnowledgeSource(UUID.randomUUID(), "doc1.pdf", SourceType.FILE),
            createMockKnowledgeSource(UUID.randomUUID(), "doc2.pdf", SourceType.FILE)
        )
        val query = GetKnowledgeSourcesQuery(knowledgeBaseId = knowledgeBaseId, pageable = pageable)

        every {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = pageable
            )
        } returns PageImpl(sources, pageable, 5)

        val result = handler.handle(query)

        assertEquals(2, result.content.size)
        assertEquals(5, result.totalElements)
        assertEquals(3, result.totalPages)
        assertEquals(0, result.number)
    }

    @Test
    fun `should map knowledge source to summary dto correctly`() {
        val sourceId = UUID.randomUUID()
        val createdAt = Instant.now().minusSeconds(3600)
        val processedAt = Instant.now().minusSeconds(1800)
        val source = KnowledgeSource(
            id = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            sourceType = SourceType.URL,
            fileName = null,
            sourceUrl = "https://example.com/page",
            title = "Example Page",
            fileSize = null,
            status = KnowledgeSourceStatus.READY,
            chunkCount = 5,
            embeddingCount = 5,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            processedAt = processedAt
        )

        val pageable = PageRequest.of(0, 10)
        val query = GetKnowledgeSourcesQuery(knowledgeBaseId = knowledgeBaseId, pageable = pageable)

        every {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = pageable
            )
        } returns PageImpl(listOf(source))

        val result = handler.handle(query)

        assertEquals(1, result.content.size)
        val dto = result.content[0]
        assertEquals(sourceId, dto.id)
        assertEquals(SourceType.URL, dto.sourceType)
        assertEquals("https://example.com/page", dto.sourceUrl)
        assertEquals("Example Page", dto.title)
        assertEquals(KnowledgeSourceStatus.READY, dto.status)
        assertEquals(5, dto.chunkCount)
        assertEquals(5, dto.embeddingCount)
        assertEquals(createdAt, dto.createdAt)
        assertEquals(processedAt, dto.processedAt)
    }

    @Test
    fun `should exclude deleted sources`() {
        val pageable = PageRequest.of(0, 20)
        val query = GetKnowledgeSourcesQuery(knowledgeBaseId = knowledgeBaseId, pageable = pageable)

        every {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = pageable
            )
        } returns PageImpl(emptyList())

        handler.handle(query)

        verify {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = any()
            )
        }
    }

    @Test
    fun `should handle different page sizes`() {
        val pageable = PageRequest.of(1, 5)
        val sources = listOf(
            createMockKnowledgeSource(UUID.randomUUID(), "doc1.pdf", SourceType.FILE)
        )
        val query = GetKnowledgeSourcesQuery(knowledgeBaseId = knowledgeBaseId, pageable = pageable)

        every {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = pageable
            )
        } returns PageImpl(sources, pageable, 6)

        val result = handler.handle(query)

        assertEquals(1, result.content.size)
        assertEquals(6, result.totalElements)
        assertEquals(2, result.totalPages)
        assertEquals(1, result.number)
        assertEquals(5, result.size)
    }

    @Test
    fun `should return sources with various statuses except deleted`() {
        val pageable = PageRequest.of(0, 20)
        val sources = listOf(
            createMockKnowledgeSource(UUID.randomUUID(), "pending.pdf", SourceType.FILE, KnowledgeSourceStatus.PENDING),
            createMockKnowledgeSource(UUID.randomUUID(), "processing.pdf", SourceType.FILE, KnowledgeSourceStatus.PROCESSING),
            createMockKnowledgeSource(UUID.randomUUID(), "ready.pdf", SourceType.FILE, KnowledgeSourceStatus.READY),
            createMockKnowledgeSource(UUID.randomUUID(), "failed.pdf", SourceType.FILE, KnowledgeSourceStatus.FAILED)
        )
        val query = GetKnowledgeSourcesQuery(knowledgeBaseId = knowledgeBaseId, pageable = pageable)

        every {
            knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
                knowledgeBaseId = knowledgeBaseId,
                status = KnowledgeSourceStatus.DELETED,
                pageable = pageable
            )
        } returns PageImpl(sources)

        val result = handler.handle(query)

        assertEquals(4, result.content.size)
        assertEquals(KnowledgeSourceStatus.PENDING, result.content[0].status)
        assertEquals(KnowledgeSourceStatus.PROCESSING, result.content[1].status)
        assertEquals(KnowledgeSourceStatus.READY, result.content[2].status)
        assertEquals(KnowledgeSourceStatus.FAILED, result.content[3].status)
    }

    private fun createMockKnowledgeSource(
        id: UUID,
        fileName: String?,
        sourceType: SourceType,
        status: KnowledgeSourceStatus = KnowledgeSourceStatus.READY
    ): KnowledgeSource {
        return KnowledgeSource(
            id = id,
            knowledgeBaseId = knowledgeBaseId,
            sourceType = sourceType,
            fileName = fileName,
            title = fileName ?: "Untitled",
            fileSize = 1024L,
            status = status,
            chunkCount = 10,
            embeddingCount = 10,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
