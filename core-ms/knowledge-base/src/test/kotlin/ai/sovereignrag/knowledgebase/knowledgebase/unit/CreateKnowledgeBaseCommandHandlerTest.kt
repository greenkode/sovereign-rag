package ai.sovereignrag.knowledgebase.knowledgebase.unit

import ai.sovereignrag.commons.process.CreateNewProcessPayload
import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.ProcessRequestDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.knowledgebase.knowledgebase.command.CreateKnowledgeBaseCommand
import ai.sovereignrag.knowledgebase.knowledgebase.command.CreateKnowledgeBaseCommandHandler
import ai.sovereignrag.knowledgebase.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.knowledgebase.knowledgebase.gateway.IdentityServiceGateway
import ai.sovereignrag.knowledgebase.knowledgebase.gateway.KBOAuthCredentials
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import ai.sovereignrag.knowledgebase.knowledgebase.service.OrganizationDatabaseService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateKnowledgeBaseCommandHandlerTest {

    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService = mockk()
    private val organizationDatabaseService: OrganizationDatabaseService = mockk()
    private val identityServiceGateway: IdentityServiceGateway = mockk()
    private val processGateway: ProcessGateway = mockk()

    private lateinit var handler: CreateKnowledgeBaseCommandHandler

    private val userId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val processId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = CreateKnowledgeBaseCommandHandler(
            knowledgeBaseRegistryService,
            organizationDatabaseService,
            identityServiceGateway,
            processGateway
        )
    }

    @Test
    fun `should create knowledge base successfully`() {
        val command = CreateKnowledgeBaseCommand(
            name = "Test Knowledge Base",
            organizationId = organizationId,
            createdByUserId = userId.toString(),
            description = "Test description"
        )

        val processPayloadSlot = slot<CreateNewProcessPayload>()
        val mockProcess = createMockProcess()
        val mockKnowledgeBase = createMockKnowledgeBase("Test Knowledge Base")
        val mockCredentials = KBOAuthCredentials(
            clientId = "kb_test123",
            clientSecret = "secret123",
            knowledgeBaseId = mockKnowledgeBase.id
        )

        every { processGateway.createProcess(capture(processPayloadSlot)) } returns mockProcess
        every { organizationDatabaseService.ensureOrganizationDatabaseExists(organizationId) } just runs
        every { organizationDatabaseService.createKnowledgeBaseSchema(organizationId, any()) } just runs
        every { knowledgeBaseRegistryService.createKnowledgeBase(any(), any(), any(), any(), any()) } returns mockKnowledgeBase
        every { identityServiceGateway.createKBOAuthClient(organizationId, any(), any()) } returns mockCredentials
        every { knowledgeBaseRegistryService.updateOauthClientId(any(), any()) } just runs
        every { processGateway.completeProcess(any(), any()) } just runs

        val result = handler.handle(command)

        assertNotNull(result)
        assertEquals("Test Knowledge Base", result.knowledgeBase.name)
        assertEquals("kb_test123", result.knowledgeBase.oauthClientId)
        assertEquals("kb_test123", result.clientId)
        assertEquals("secret123", result.clientSecret)

        val capturedPayload = processPayloadSlot.captured
        assertEquals(ProcessType.KNOWLEDGE_BASE_CREATION, capturedPayload.type)
        assertEquals(ProcessChannel.BUSINESS_WEB, capturedPayload.channel)
        assertEquals(userId, capturedPayload.userId)

        verify { processGateway.createProcess(any()) }
        verify { organizationDatabaseService.ensureOrganizationDatabaseExists(organizationId) }
        verify { organizationDatabaseService.createKnowledgeBaseSchema(organizationId, any()) }
        verify { knowledgeBaseRegistryService.createKnowledgeBase(any(), eq("Test Knowledge Base"), eq(organizationId), any(), any()) }
        verify { identityServiceGateway.createKBOAuthClient(organizationId, any(), eq("Test Knowledge Base")) }
        verify { processGateway.completeProcess(processId, 1L) }
    }

    @Test
    fun `should include organization id in process data`() {
        val command = CreateKnowledgeBaseCommand(
            name = "My KB",
            organizationId = organizationId,
            createdByUserId = userId.toString()
        )

        val processPayloadSlot = slot<CreateNewProcessPayload>()
        val mockProcess = createMockProcess()
        val mockKnowledgeBase = createMockKnowledgeBase("My KB")
        val mockCredentials = KBOAuthCredentials(
            clientId = "kb_test456",
            clientSecret = "secret456",
            knowledgeBaseId = mockKnowledgeBase.id
        )

        every { processGateway.createProcess(capture(processPayloadSlot)) } returns mockProcess
        every { organizationDatabaseService.ensureOrganizationDatabaseExists(organizationId) } just runs
        every { organizationDatabaseService.createKnowledgeBaseSchema(organizationId, any()) } just runs
        every { knowledgeBaseRegistryService.createKnowledgeBase(any(), any(), any(), any(), any()) } returns mockKnowledgeBase
        every { identityServiceGateway.createKBOAuthClient(organizationId, any(), any()) } returns mockCredentials
        every { knowledgeBaseRegistryService.updateOauthClientId(any(), any()) } just runs
        every { processGateway.completeProcess(any(), any()) } just runs

        handler.handle(command)

        val capturedPayload = processPayloadSlot.captured
        assertNotNull(capturedPayload.data)
        assertEquals(organizationId.toString(), capturedPayload.data[ProcessRequestDataName.ORGANIZATION_ID])
        assertEquals("My KB", capturedPayload.data[ProcessRequestDataName.KNOWLEDGE_BASE_NAME])
    }

    @Test
    fun `should set user as stakeholder`() {
        val command = CreateKnowledgeBaseCommand(
            name = "KB with Stakeholder",
            organizationId = organizationId,
            createdByUserId = userId.toString()
        )

        val processPayloadSlot = slot<CreateNewProcessPayload>()
        val mockProcess = createMockProcess()
        val mockKnowledgeBase = createMockKnowledgeBase("KB with Stakeholder")
        val mockCredentials = KBOAuthCredentials(
            clientId = "kb_test789",
            clientSecret = "secret789",
            knowledgeBaseId = mockKnowledgeBase.id
        )

        every { processGateway.createProcess(capture(processPayloadSlot)) } returns mockProcess
        every { organizationDatabaseService.ensureOrganizationDatabaseExists(organizationId) } just runs
        every { organizationDatabaseService.createKnowledgeBaseSchema(organizationId, any()) } just runs
        every { knowledgeBaseRegistryService.createKnowledgeBase(any(), any(), any(), any(), any()) } returns mockKnowledgeBase
        every { identityServiceGateway.createKBOAuthClient(organizationId, any(), any()) } returns mockCredentials
        every { knowledgeBaseRegistryService.updateOauthClientId(any(), any()) } just runs
        every { processGateway.completeProcess(any(), any()) } just runs

        handler.handle(command)

        val capturedPayload = processPayloadSlot.captured
        assertEquals(userId.toString(), capturedPayload.stakeholders[ProcessStakeholderType.ACTOR_USER])
    }

    private fun createMockProcess(): ProcessDto {
        return ProcessDto(
            id = 1L,
            publicId = processId,
            state = ProcessState.PENDING,
            type = ProcessType.KNOWLEDGE_BASE_CREATION,
            channel = ProcessChannel.BUSINESS_WEB,
            createdDate = Instant.now(),
            requests = listOf(
                ProcessRequestDto(
                    id = 1L,
                    type = ProcessRequestType.CREATE_NEW_PROCESS,
                    state = ProcessState.PENDING,
                    stakeholders = emptyMap(),
                    data = emptyMap()
                )
            )
        )
    }

    private fun createMockKnowledgeBase(name: String): KnowledgeBase {
        val kbId = UUID.randomUUID().toString()
        return KnowledgeBase(
            id = kbId,
            name = name,
            organizationId = organizationId,
            schemaName = "kb_${kbId.replace("-", "_").take(32)}",
            status = KnowledgeBaseStatus.ACTIVE,
            maxKnowledgeSources = 10000,
            maxEmbeddings = 100000,
            maxRequestsPerDay = 1000,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
