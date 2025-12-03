package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class SubmitScrapeJobCommand(
    val organizationId: UUID,
    val url: String,
    val knowledgeBaseId: UUID?,
    val depth: Int = 1,
    val maxPages: Int = 10
) : Command<IngestionJobResponse>
