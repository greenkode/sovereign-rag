package ai.sovereignrag.kb.knowledgebase.command

import ai.sovereignrag.kb.knowledgebase.dto.CreateKnowledgeBaseResult
import an.awesome.pipelinr.Command
import java.util.UUID

data class CreateKnowledgeBaseCommand(
    val name: String,
    val organizationId: UUID,
    val createdByUserId: String,
    val description: String? = null
) : Command<CreateKnowledgeBaseResult>
