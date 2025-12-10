package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class SubmitTextInputCommand(
    val organizationId: UUID,
    val content: String,
    val title: String?,
    val knowledgeBaseId: UUID?,
    val metadata: Map<String, String>?
) : Command<IngestionJobResponse>
