package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class ConfirmUploadCommand(
    val tenantId: UUID,
    val jobId: UUID
) : Command<IngestionJobResponse>
