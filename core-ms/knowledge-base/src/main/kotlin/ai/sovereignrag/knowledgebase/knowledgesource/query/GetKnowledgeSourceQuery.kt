package ai.sovereignrag.knowledgebase.knowledgesource.query

import ai.sovereignrag.knowledgebase.knowledgesource.dto.KnowledgeSourceDto
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetKnowledgeSourceQuery(
    val knowledgeBaseId: UUID,
    val sourceId: UUID
) : Command<KnowledgeSourceDto?>
