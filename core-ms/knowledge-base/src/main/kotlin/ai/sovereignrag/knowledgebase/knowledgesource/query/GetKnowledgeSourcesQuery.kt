package ai.sovereignrag.knowledgebase.knowledgesource.query

import ai.sovereignrag.knowledgebase.knowledgesource.dto.KnowledgeSourceSummaryDto
import an.awesome.pipelinr.Command
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

data class GetKnowledgeSourcesQuery(
    val knowledgeBaseId: UUID,
    val pageable: Pageable
) : Command<Page<KnowledgeSourceSummaryDto>>
