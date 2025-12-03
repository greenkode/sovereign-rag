package ai.sovereignrag.ingestion.core.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class CancelJobCommand(
    val organizationId: UUID,
    val jobId: UUID
) : Command<CancelJobResult>

data class CancelJobResult(
    val success: Boolean,
    val message: String
)
