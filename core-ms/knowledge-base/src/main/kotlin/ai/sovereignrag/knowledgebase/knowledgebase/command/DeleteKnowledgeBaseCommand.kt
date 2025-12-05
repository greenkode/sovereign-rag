package ai.sovereignrag.knowledgebase.knowledgebase.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class DeleteKnowledgeBaseCommand(
    val knowledgeBaseId: String,
    val organizationId: UUID
) : Command<DeleteKnowledgeBaseResult>

data class DeleteKnowledgeBaseResult(
    val success: Boolean,
    val message: String
)
