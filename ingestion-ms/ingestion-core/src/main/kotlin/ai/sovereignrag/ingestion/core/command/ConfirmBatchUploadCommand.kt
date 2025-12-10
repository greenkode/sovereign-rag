package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class ConfirmBatchUploadCommand(
    val organizationId: UUID,
    val batchJobId: UUID
) : Command<IngestionJobResponse>
