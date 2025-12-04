package ai.sovereignrag.knowledgebase.knowledgebase.query

import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseSummaryDto
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
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
            KnowledgeBaseSummaryDto(
                id = kb.id,
                name = kb.name,
                description = kb.description,
                organizationId = kb.organizationId,
                status = kb.status,
                documentCount = 0,
                lastActiveAt = kb.lastActiveAt,
                createdAt = kb.createdAt
            )
        }
    }
}
