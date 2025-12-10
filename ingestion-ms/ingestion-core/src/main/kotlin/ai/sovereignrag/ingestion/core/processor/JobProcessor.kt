package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType

interface JobProcessor {
    fun supports(jobType: JobType): Boolean
    fun process(job: IngestionJob)
}
