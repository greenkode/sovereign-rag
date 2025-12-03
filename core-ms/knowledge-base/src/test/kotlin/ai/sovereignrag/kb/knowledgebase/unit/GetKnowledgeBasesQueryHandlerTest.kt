package ai.sovereignrag.kb.knowledgebase.unit

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.kb.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.kb.knowledgebase.query.GetKnowledgeBasesQuery
import ai.sovereignrag.kb.knowledgebase.query.GetKnowledgeBasesQueryHandler
import ai.sovereignrag.kb.knowledgebase.service.KnowledgeBaseRegistryService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetKnowledgeBasesQueryHandlerTest {

    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService = mockk()

    private lateinit var handler: GetKnowledgeBasesQueryHandler

    private val organizationId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = GetKnowledgeBasesQueryHandler(knowledgeBaseRegistryService)
    }

    @Test
    fun `should return empty list when no knowledge bases exist`() {
        val query = GetKnowledgeBasesQuery(organizationId = organizationId)

        every { knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(organizationId, null) } returns emptyList()

        val result = handler.handle(query)

        assertTrue(result.isEmpty())
        verify { knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(organizationId, null) }
    }

    @Test
    fun `should return knowledge bases for organization`() {
        val query = GetKnowledgeBasesQuery(organizationId = organizationId)
        val knowledgeBases = listOf(
            createMockKnowledgeBase("KB 1", KnowledgeBaseStatus.ACTIVE),
            createMockKnowledgeBase("KB 2", KnowledgeBaseStatus.ACTIVE)
        )

        every { knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(organizationId, null) } returns knowledgeBases

        val result = handler.handle(query)

        assertEquals(2, result.size)
        assertEquals("KB 1", result[0].name)
        assertEquals("KB 2", result[1].name)
    }

    @Test
    fun `should filter by status when provided`() {
        val query = GetKnowledgeBasesQuery(organizationId = organizationId, status = KnowledgeBaseStatus.ACTIVE)
        val knowledgeBases = listOf(createMockKnowledgeBase("Active KB", KnowledgeBaseStatus.ACTIVE))

        every { knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(organizationId, KnowledgeBaseStatus.ACTIVE) } returns knowledgeBases

        val result = handler.handle(query)

        assertEquals(1, result.size)
        assertEquals("Active KB", result[0].name)
        assertEquals(KnowledgeBaseStatus.ACTIVE, result[0].status)
        verify { knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(organizationId, KnowledgeBaseStatus.ACTIVE) }
    }

    @Test
    fun `should map knowledge base fields correctly to dto`() {
        val query = GetKnowledgeBasesQuery(organizationId = organizationId)
        val kb = createMockKnowledgeBase("Test KB", KnowledgeBaseStatus.SUSPENDED)

        every { knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(organizationId, null) } returns listOf(kb)

        val result = handler.handle(query)

        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(kb.id, dto.id)
        assertEquals("Test KB", dto.name)
        assertEquals(KnowledgeBaseStatus.SUSPENDED, dto.status)
        assertEquals(organizationId, dto.organizationId)
    }

    private fun createMockKnowledgeBase(name: String, status: KnowledgeBaseStatus): KnowledgeBase {
        val kbId = UUID.randomUUID().toString()
        return KnowledgeBase(
            id = kbId,
            name = name,
            organizationId = organizationId,
            schemaName = "kb_${kbId.replace("-", "_").take(32)}",
            status = status,
            maxDocuments = 10000,
            maxEmbeddings = 100000,
            maxRequestsPerDay = 1000,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
