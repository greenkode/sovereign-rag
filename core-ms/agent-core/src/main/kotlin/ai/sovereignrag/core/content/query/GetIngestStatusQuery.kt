package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import ai.sovereignrag.content.dto.IngestStatusResponse

data class GetIngestStatusQuery(
    val taskId: String
) : Command<IngestStatusResponse>
