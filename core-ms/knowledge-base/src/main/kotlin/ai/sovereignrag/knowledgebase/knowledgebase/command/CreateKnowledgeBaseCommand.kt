package ai.sovereignrag.knowledgebase.knowledgebase.command

import ai.sovereignrag.knowledgebase.knowledgebase.dto.CreateKnowledgeBaseResult
import an.awesome.pipelinr.Command
import java.util.UUID

data class CreateKnowledgeBaseCommand(
    val name: String,
    val organizationId: UUID,
    val createdByUserId: String,
    val description: String? = null,
    val regionCode: String? = null,
    val embeddingModelId: String? = null,
    val llmModelId: String? = null,
    val requiresEncryption: Boolean? = null
) : Command<CreateKnowledgeBaseResult>
