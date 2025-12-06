package ai.sovereignrag.knowledgebase.knowledgebase.command

import ai.sovereignrag.commons.process.CreateNewProcessPayload
import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.knowledgebase.knowledgebase.dto.CreateKnowledgeBaseResult
import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseDto
import ai.sovereignrag.knowledgebase.knowledgebase.gateway.IdentityServiceGateway
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import ai.sovereignrag.knowledgebase.knowledgebase.service.OrganizationDatabaseService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class CreateKnowledgeBaseCommandHandler(
    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService,
    private val organizationDatabaseService: OrganizationDatabaseService,
    private val identityServiceGateway: IdentityServiceGateway,
    private val processGateway: ProcessGateway
) : Command.Handler<CreateKnowledgeBaseCommand, CreateKnowledgeBaseResult> {

    override fun handle(command: CreateKnowledgeBaseCommand): CreateKnowledgeBaseResult {
        log.info { "Creating knowledge base: ${command.name} for organization: ${command.organizationId}" }

        val knowledgeBaseId = UUID.randomUUID().toString()
        val schemaName = "kb_${knowledgeBaseId.replace("-", "_").take(32)}"
        val processPublicId = UUID.randomUUID()
        val userId = UUID.fromString(command.createdByUserId)

        val processPayload = CreateNewProcessPayload(
            userId = userId,
            publicId = processPublicId,
            type = ProcessType.KNOWLEDGE_BASE_CREATION,
            description = "Creating knowledge base: ${command.name}",
            initialState = ProcessState.PENDING,
            requestState = ProcessState.PENDING,
            channel = ProcessChannel.BUSINESS_WEB,
            externalReference = knowledgeBaseId,
            stakeholders = mapOf(
                ProcessStakeholderType.ACTOR_USER to command.createdByUserId
            ),
            data = mapOf(
                ProcessRequestDataName.KNOWLEDGE_BASE_ID to knowledgeBaseId,
                ProcessRequestDataName.KNOWLEDGE_BASE_NAME to command.name,
                ProcessRequestDataName.ORGANIZATION_ID to command.organizationId.toString()
            )
        )

        val process = processGateway.createProcess(processPayload)
        log.debug { "Created process ${process.publicId} for knowledge base creation" }

        organizationDatabaseService.ensureOrganizationDatabaseExists(command.organizationId)
        organizationDatabaseService.createKnowledgeBaseSchema(command.organizationId, schemaName)

        val knowledgeBase = knowledgeBaseRegistryService.createKnowledgeBase(
            id = knowledgeBaseId,
            name = command.name,
            organizationId = command.organizationId,
            schemaName = schemaName,
            description = command.description
        )

        val credentials = identityServiceGateway.createKBOAuthClient(
            organizationId = command.organizationId,
            knowledgeBaseId = knowledgeBaseId,
            name = command.name
        )

        knowledgeBaseRegistryService.updateOauthClientId(knowledgeBaseId, credentials.clientId)

        processGateway.completeProcess(
            processId = process.publicId,
            requestId = process.requests.first().id
        )

        log.info { "Knowledge base created successfully: ${knowledgeBase.id}" }

        return CreateKnowledgeBaseResult(
            knowledgeBase = KnowledgeBaseDto(
                id = knowledgeBase.id,
                name = knowledgeBase.name,
                description = knowledgeBase.description,
                organizationId = knowledgeBase.organizationId,
                oauthClientId = credentials.clientId,
                status = knowledgeBase.status,
                knowledgeSourceCount = 0,
                embeddingCount = 0,
                queryCount = 0,
                maxKnowledgeSources = knowledgeBase.maxKnowledgeSources,
                maxEmbeddings = knowledgeBase.maxEmbeddings,
                maxRequestsPerDay = knowledgeBase.maxRequestsPerDay,
                createdAt = knowledgeBase.createdAt,
                updatedAt = knowledgeBase.updatedAt,
                lastActiveAt = knowledgeBase.lastActiveAt
            ),
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret
        )
    }
}
