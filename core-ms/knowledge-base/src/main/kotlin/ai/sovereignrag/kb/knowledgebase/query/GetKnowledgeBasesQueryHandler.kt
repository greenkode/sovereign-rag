package ai.sovereignrag.kb.knowledgebase.query

import ai.sovereignrag.kb.knowledgebase.dto.KnowledgeBaseSummaryDto
import ai.sovereignrag.kb.knowledgebase.service.KnowledgeBaseRegistryService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetKnowledgeBasesQueryHandler(
    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService
) : Command.Handler<GetKnowledgeBasesQuery, List<KnowledgeBaseSummaryDto>> {

    override fun handle(query: GetKnowledgeBasesQuery): List<KnowledgeBaseSummaryDto> {
        log.debug { "Fetching knowledge bases for organization: ${query.organizationId}" }

        val knowledgeBases = knowledgeBaseRegistryService.listKnowledgeBasesByOrganization(
            organizationId = query.organizationId,
            status = query.status
        )

        return knowledgeBases.map { kb ->
            KnowledgeBaseSummaryDto.from(kb)
        }
    }
}
