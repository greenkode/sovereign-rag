package ai.sovereignrag.knowledgebase.knowledgebase.unit

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.knowledgebase.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.knowledgebase.knowledgebase.query.GetKnowledgeBaseQuery
import ai.sovereignrag.knowledgebase.knowledgebase.query.GetKnowledgeBaseQueryHandler
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
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

class GetKnowledgeBaseQueryHandlerTest {

    private val knowledgeBaseRepository: KnowledgeBaseRepository = mockk()

    private lateinit var handler: GetKnowledgeBaseQueryHandler

    private val organizationId = UUID.randomUUID()
    private val knowledgeBaseId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        handler = GetKnowledgeBaseQueryHandler(knowledgeBaseRepository)
    }

    @Test
    fun `should return null when knowledge base does not exist`() {
        val query = GetKnowledgeBaseQuery(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns null

        val result = handler.handle(query)

        assertNull(result)
        verify { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) }
    }

    @Test
    fun `should return null when knowledge base belongs to different organization`() {
        val query = GetKnowledgeBaseQuery(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )
        val differentOrganizationId = UUID.randomUUID()
        val knowledgeBase = createMockKnowledgeBase("Other Org KB", differentOrganizationId)

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns knowledgeBase

        val result = handler.handle(query)

        assertNull(result)
    }

    @Test
    fun `should return knowledge base when found and organization matches`() {
        val query = GetKnowledgeBaseQuery(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )
        val knowledgeBase = createMockKnowledgeBase("My KB", organizationId)

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns knowledgeBase

        val result = handler.handle(query)

        assertNotNull(result)
        assertEquals("My KB", result.name)
        assertEquals(KnowledgeBaseStatus.ACTIVE, result.status)
    }

    @Test
    fun `should map all knowledge base fields to dto`() {
        val query = GetKnowledgeBaseQuery(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )
        val knowledgeBase = createMockKnowledgeBase("Full Details KB", organizationId).copy(
            maxKnowledgeSources = 5000,
            maxEmbeddings = 50000,
            maxRequestsPerDay = 500,
            lastActiveAt = Instant.now().minusSeconds(3600)
        )

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns knowledgeBase

        val result = handler.handle(query)

        assertNotNull(result)
        assertEquals(knowledgeBase.id, result.id)
        assertEquals("Full Details KB", result.name)
        assertEquals(KnowledgeBaseStatus.ACTIVE, result.status)
        assertEquals(5000, result.maxKnowledgeSources)
        assertEquals(50000, result.maxEmbeddings)
        assertEquals(500, result.maxRequestsPerDay)
        assertNotNull(result.lastActiveAt)
    }

    private fun createMockKnowledgeBase(name: String, orgId: UUID): KnowledgeBase {
        return KnowledgeBase(
            id = knowledgeBaseId,
            name = name,
            organizationId = orgId,
            schemaName = "kb_${knowledgeBaseId.replace("-", "_").take(32)}",
            status = KnowledgeBaseStatus.ACTIVE,
            maxKnowledgeSources = 10000,
            maxEmbeddings = 100000,
            maxRequestsPerDay = 1000,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
