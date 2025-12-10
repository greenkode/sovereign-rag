package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.repository.RemiMetricsRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@Component
class GetRemiMetricsQueryHandler(
    private val remiMetricsRepository: RemiMetricsRepository
) : Command.Handler<GetRemiMetricsQuery, RemiMetricsPagedResult> {

    @Transactional(readOnly = true)
    override fun handle(query: GetRemiMetricsQuery): RemiMetricsPagedResult {
        val since = Instant.now().minus(query.days.toLong(), ChronoUnit.DAYS)
        val pageable = PageRequest.of(query.page, query.size)

        log.debug {
            "Querying REMI metrics for org=${query.organizationId}, kb=${query.knowledgeBaseId}, " +
            "filter=${query.filter}, since=$since"
        }

        val page = when (query.filter) {
            RemiFilter.HALLUCINATIONS -> remiMetricsRepository.findHallucinations(
                organizationId = query.organizationId,
                knowledgeBaseId = query.knowledgeBaseId,
                since = since,
                pageable = pageable
            )
            RemiFilter.MISSING_KNOWLEDGE -> remiMetricsRepository.findMissingKnowledge(
                organizationId = query.organizationId,
                knowledgeBaseId = query.knowledgeBaseId,
                since = since,
                pageable = pageable
            )
            else -> remiMetricsRepository.findByOrganizationIdAndKnowledgeBaseId(
                organizationId = query.organizationId,
                knowledgeBaseId = query.knowledgeBaseId,
                since = since,
                pageable = pageable
            )
        }

        return RemiMetricsPagedResult(
            metrics = page.content.map { it.toResult() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }
}
