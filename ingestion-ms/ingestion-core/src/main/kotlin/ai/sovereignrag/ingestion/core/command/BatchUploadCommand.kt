package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.BatchFileInfo
import ai.sovereignrag.ingestion.commons.dto.BatchUploadResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class BatchUploadCommand(
    val organizationId: UUID,
    val files: List<BatchFileInfo>,
    val knowledgeBaseId: UUID?
) : Command<BatchUploadResponse>
