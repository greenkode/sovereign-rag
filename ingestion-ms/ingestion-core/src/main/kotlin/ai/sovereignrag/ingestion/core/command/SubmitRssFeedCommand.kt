package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class SubmitRssFeedCommand(
    val organizationId: UUID,
    val feedUrl: String,
    val knowledgeBaseId: UUID?,
    val sourceName: String?,
    val maxItems: Int,
    val includeFullContent: Boolean
) : Command<IngestionJobResponse>
