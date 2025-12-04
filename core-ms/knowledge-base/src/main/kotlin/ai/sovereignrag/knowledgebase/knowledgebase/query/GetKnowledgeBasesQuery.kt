package ai.sovereignrag.knowledgebase.knowledgebase.query

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseSummaryDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetKnowledgeBasesQuery(
    val organizationId: UUID,
    val status: KnowledgeBaseStatus? = null
) : Command<List<KnowledgeBaseSummaryDto>>
