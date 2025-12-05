package ai.sovereignrag.knowledgebase.knowledgebase.command

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.knowledgebase.knowledgebase.gateway.IdentityServiceGateway
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DeleteKnowledgeBaseCommandHandler(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService,
    private val identityServiceGateway: IdentityServiceGateway
) : Command.Handler<DeleteKnowledgeBaseCommand, DeleteKnowledgeBaseResult> {

    override fun handle(command: DeleteKnowledgeBaseCommand): DeleteKnowledgeBaseResult {
        log.info { "Deleting knowledge base: ${command.knowledgeBaseId}" }

        val knowledgeBase = knowledgeBaseRepository.findByIdAndDeletedAtIsNull(command.knowledgeBaseId)
            ?: throw KnowledgeBaseNotFoundException("Knowledge base not found: ${command.knowledgeBaseId}")

        if (knowledgeBase.organizationId != command.organizationId) {
            throw KnowledgeBaseNotFoundException("Knowledge base not found: ${command.knowledgeBaseId}")
        }

        identityServiceGateway.revokeKBOAuthClient(command.knowledgeBaseId)
            .also { log.debug { "OAuth client revoked for knowledge base: ${command.knowledgeBaseId}" } }

        knowledgeBaseRegistryService.deleteKnowledgeBase(command.knowledgeBaseId)

        log.info { "Knowledge base deleted: ${command.knowledgeBaseId}" }

        return DeleteKnowledgeBaseResult(
            success = true,
            message = "knowledge_base.deleted_successfully"
        )
    }
}
