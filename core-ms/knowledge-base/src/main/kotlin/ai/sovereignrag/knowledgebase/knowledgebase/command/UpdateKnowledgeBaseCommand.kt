package ai.sovereignrag.knowledgebase.knowledgebase.command

import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class UpdateKnowledgeBaseCommand(
    val knowledgeBaseId: String,
    val organizationId: UUID,
    val name: String?,
    val description: String?
) : Command<KnowledgeBaseDto>
