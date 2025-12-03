package ai.sovereignrag.kb.knowledgebase.query

import ai.sovereignrag.kb.knowledgebase.dto.KnowledgeBaseDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetKnowledgeBaseQuery(
    val knowledgeBaseId: String,
    val organizationId: UUID
) : Command<KnowledgeBaseDto?>
