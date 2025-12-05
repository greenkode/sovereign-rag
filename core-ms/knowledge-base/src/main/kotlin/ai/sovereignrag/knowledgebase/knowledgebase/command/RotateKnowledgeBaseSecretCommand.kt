package ai.sovereignrag.knowledgebase.knowledgebase.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class RotateKnowledgeBaseSecretCommand(
    val knowledgeBaseId: String,
    val organizationId: UUID
) : Command<RotateSecretResult>

data class RotateSecretResult(
    val clientId: String,
    val clientSecret: String
)
