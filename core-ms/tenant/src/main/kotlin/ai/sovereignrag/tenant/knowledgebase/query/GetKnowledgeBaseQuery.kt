package ai.sovereignrag.tenant.knowledgebase.query

import ai.sovereignrag.tenant.knowledgebase.dto.KnowledgeBaseDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetKnowledgeBaseQuery(
    val knowledgeBaseId: String,
    val organizationId: UUID
) : Command<KnowledgeBaseDto?>
