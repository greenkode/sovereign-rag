package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetJobQuery(
    val tenantId: UUID,
    val jobId: UUID
) : Command<IngestionJobResponse>
