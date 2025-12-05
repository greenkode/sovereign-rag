package ai.sovereignrag.knowledgebase.knowledgebase.unit

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.knowledgebase.knowledgebase.command.DeleteKnowledgeBaseCommand
import ai.sovereignrag.knowledgebase.knowledgebase.command.DeleteKnowledgeBaseCommandHandler
import ai.sovereignrag.knowledgebase.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.knowledgebase.knowledgebase.gateway.IdentityServiceGateway
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import ai.sovereignrag.knowledgebase.knowledgebase.service.OrganizationDatabaseService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.HttpClientErrorException
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteKnowledgeBaseCommandHandlerTest {

    private val knowledgeBaseRepository: KnowledgeBaseRepository = mockk()
    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService = mockk()
    private val identityServiceGateway: IdentityServiceGateway = mockk()
    private val organizationDatabaseService: OrganizationDatabaseService = mockk()

    private lateinit var handler: DeleteKnowledgeBaseCommandHandler

    private val organizationId = UUID.randomUUID()
    private val knowledgeBaseId = UUID.randomUUID().toString()
    private val schemaName = "kb_test_schema"

    @BeforeEach
    fun setup() {
        handler = DeleteKnowledgeBaseCommandHandler(
            knowledgeBaseRepository,
            knowledgeBaseRegistryService,
            identityServiceGateway,
            organizationDatabaseService
        )
    }

    @Test
    fun `should delete knowledge base successfully`() {
        val command = DeleteKnowledgeBaseCommand(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )

        val mockKnowledgeBase = createMockKnowledgeBase()

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns mockKnowledgeBase
        every { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) } just runs
        every { organizationDatabaseService.dropKnowledgeBaseSchema(organizationId, schemaName) } just runs
        every { knowledgeBaseRegistryService.deleteKnowledgeBase(knowledgeBaseId) } just runs

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("knowledge_base.deleted_successfully", result.message)

        verify { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) }
        verify { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) }
        verify { organizationDatabaseService.dropKnowledgeBaseSchema(organizationId, schemaName) }
        verify { knowledgeBaseRegistryService.deleteKnowledgeBase(knowledgeBaseId) }
    }

    @Test
    fun `should throw exception when knowledge base not found`() {
        val command = DeleteKnowledgeBaseCommand(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns null

        assertThrows<KnowledgeBaseNotFoundException> {
            handler.handle(command)
        }
    }

    @Test
    fun `should throw exception when organization id does not match`() {
        val differentOrgId = UUID.randomUUID()
        val command = DeleteKnowledgeBaseCommand(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = differentOrgId
        )

        val mockKnowledgeBase = createMockKnowledgeBase()

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns mockKnowledgeBase

        assertThrows<KnowledgeBaseNotFoundException> {
            handler.handle(command)
        }
    }

    @Test
    fun `should continue deletion when OAuth client not found`() {
        val command = DeleteKnowledgeBaseCommand(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )

        val mockKnowledgeBase = createMockKnowledgeBase()

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns mockKnowledgeBase
        every { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) } throws HttpClientErrorException.NotFound.create(
            org.springframework.http.HttpStatus.NOT_FOUND,
            "Not Found",
            org.springframework.http.HttpHeaders.EMPTY,
            ByteArray(0),
            null
        )
        every { organizationDatabaseService.dropKnowledgeBaseSchema(organizationId, schemaName) } just runs
        every { knowledgeBaseRegistryService.deleteKnowledgeBase(knowledgeBaseId) } just runs

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("knowledge_base.deleted_successfully", result.message)

        verify { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) }
        verify { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) }
        verify { organizationDatabaseService.dropKnowledgeBaseSchema(organizationId, schemaName) }
        verify { knowledgeBaseRegistryService.deleteKnowledgeBase(knowledgeBaseId) }
    }

    @Test
    fun `should rethrow other exceptions from OAuth client revocation`() {
        val command = DeleteKnowledgeBaseCommand(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )

        val mockKnowledgeBase = createMockKnowledgeBase()

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns mockKnowledgeBase
        every { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) } throws RuntimeException("Connection failed")

        assertThrows<RuntimeException> {
            handler.handle(command)
        }

        verify { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) }
        verify { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) }
        verify(exactly = 0) { organizationDatabaseService.dropKnowledgeBaseSchema(any(), any()) }
        verify(exactly = 0) { knowledgeBaseRegistryService.deleteKnowledgeBase(any()) }
    }

    @Test
    fun `should continue deletion when schema drop fails`() {
        val command = DeleteKnowledgeBaseCommand(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
        )

        val mockKnowledgeBase = createMockKnowledgeBase()

        every { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) } returns mockKnowledgeBase
        every { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) } just runs
        every { organizationDatabaseService.dropKnowledgeBaseSchema(organizationId, schemaName) } throws RuntimeException("Schema drop failed")
        every { knowledgeBaseRegistryService.deleteKnowledgeBase(knowledgeBaseId) } just runs

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("knowledge_base.deleted_successfully", result.message)

        verify { knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) }
        verify { identityServiceGateway.revokeKBOAuthClient(knowledgeBaseId) }
        verify { organizationDatabaseService.dropKnowledgeBaseSchema(organizationId, schemaName) }
        verify { knowledgeBaseRegistryService.deleteKnowledgeBase(knowledgeBaseId) }
    }

    private fun createMockKnowledgeBase(): KnowledgeBase {
        return KnowledgeBase(
            id = knowledgeBaseId,
            name = "Test Knowledge Base",
            organizationId = organizationId,
            schemaName = "kb_test_schema",
            status = KnowledgeBaseStatus.ACTIVE,
            maxKnowledgeSources = 10000,
            maxEmbeddings = 100000,
            maxRequestsPerDay = 1000,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
