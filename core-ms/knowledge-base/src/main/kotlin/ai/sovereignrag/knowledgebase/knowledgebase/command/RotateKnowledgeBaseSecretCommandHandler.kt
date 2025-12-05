package ai.sovereignrag.knowledgebase.knowledgebase.command

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.knowledgebase.knowledgebase.gateway.IdentityServiceGateway
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class RotateKnowledgeBaseSecretCommandHandler(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val identityServiceGateway: IdentityServiceGateway
) : Command.Handler<RotateKnowledgeBaseSecretCommand, RotateSecretResult> {

    override fun handle(command: RotateKnowledgeBaseSecretCommand): RotateSecretResult {
        log.info { "Rotating secret for knowledge base: ${command.knowledgeBaseId}" }

        val knowledgeBase = knowledgeBaseRepository.findByIdAndDeletedAtIsNull(command.knowledgeBaseId)
            ?: throw KnowledgeBaseNotFoundException("Knowledge base not found: ${command.knowledgeBaseId}")

        if (knowledgeBase.organizationId != command.organizationId) {
            throw KnowledgeBaseNotFoundException("Knowledge base not found: ${command.knowledgeBaseId}")
        }

        val credentials = identityServiceGateway.rotateKBOAuthClientSecret(command.knowledgeBaseId)

        log.info { "Secret rotated for knowledge base: ${command.knowledgeBaseId}" }

        return RotateSecretResult(
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret
        )
    }
}
