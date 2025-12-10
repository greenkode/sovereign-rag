package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.dto.QAPair
import an.awesome.pipelinr.Command
import java.util.UUID

data class SubmitQAPairsCommand(
    val organizationId: UUID,
    val pairs: List<QAPair>,
    val knowledgeBaseId: UUID?,
    val sourceName: String?
) : Command<IngestionJobResponse>
