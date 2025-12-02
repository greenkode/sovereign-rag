package ai.sovereignrag.tenant.knowledgebase.query

import ai.sovereignrag.commons.tenant.KnowledgeBaseStatus
import ai.sovereignrag.tenant.knowledgebase.dto.KnowledgeBaseSummaryDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetKnowledgeBasesQuery(
    val organizationId: UUID,
    val status: KnowledgeBaseStatus? = null
) : Command<List<KnowledgeBaseSummaryDto>>
