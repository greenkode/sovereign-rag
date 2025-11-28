package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.PresignedUploadResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class CreatePresignedUploadCommand(
    val tenantId: UUID,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val knowledgeBaseId: UUID?
) : Command<PresignedUploadResponse>
