package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import nl.compilot.ai.content.dto.IngestStatusResponse

data class GetIngestStatusQuery(
    val taskId: String
) : Command<IngestStatusResponse>
