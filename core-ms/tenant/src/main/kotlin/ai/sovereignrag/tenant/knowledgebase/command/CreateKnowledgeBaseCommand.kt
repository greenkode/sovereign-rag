package ai.sovereignrag.tenant.knowledgebase.command

import ai.sovereignrag.tenant.knowledgebase.dto.CreateKnowledgeBaseResult
import an.awesome.pipelinr.Command
import java.util.UUID

data class CreateKnowledgeBaseCommand(
    val name: String,
    val organizationId: UUID,
    val createdByUserId: String,
    val description: String? = null
) : Command<CreateKnowledgeBaseResult>
