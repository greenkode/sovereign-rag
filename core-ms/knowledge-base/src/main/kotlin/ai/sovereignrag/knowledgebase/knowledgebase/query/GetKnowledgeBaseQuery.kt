package ai.sovereignrag.knowledgebase.knowledgebase.query

import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetKnowledgeBaseQuery(
    val knowledgeBaseId: String,
    val organizationId: UUID
) : Command<KnowledgeBaseDto?>
