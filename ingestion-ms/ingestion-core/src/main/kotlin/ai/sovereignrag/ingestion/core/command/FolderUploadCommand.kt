package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.PresignedUploadResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class FolderUploadCommand(
    val organizationId: UUID,
    val fileName: String,
    val fileSize: Long,
    val knowledgeBaseId: UUID?,
    val preserveStructure: Boolean
) : Command<PresignedUploadResponse>
