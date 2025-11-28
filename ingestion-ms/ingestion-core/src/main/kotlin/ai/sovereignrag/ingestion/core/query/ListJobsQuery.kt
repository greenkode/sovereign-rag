package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.dto.JobListResponse
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import an.awesome.pipelinr.Command
import java.util.UUID

data class ListJobsQuery(
    val tenantId: UUID,
    val status: JobStatus? = null,
    val knowledgeBaseId: UUID? = null,
    val page: Int = 0,
    val size: Int = 20
) : Command<JobListResponse>
