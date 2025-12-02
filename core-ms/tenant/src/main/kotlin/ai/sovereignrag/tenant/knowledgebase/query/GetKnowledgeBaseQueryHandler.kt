package ai.sovereignrag.tenant.knowledgebase.query

import ai.sovereignrag.tenant.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.tenant.knowledgebase.dto.KnowledgeBaseDto
import ai.sovereignrag.tenant.knowledgebase.repository.KnowledgeBaseRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetKnowledgeBaseQueryHandler(
    private val knowledgeBaseRepository: KnowledgeBaseRepository
) : Command.Handler<GetKnowledgeBaseQuery, KnowledgeBaseDto?> {

    override fun handle(query: GetKnowledgeBaseQuery): KnowledgeBaseDto? {
        log.debug { "Fetching knowledge base: ${query.knowledgeBaseId} for organization: ${query.organizationId}" }

        val knowledgeBase = knowledgeBaseRepository.findByIdAndDeletedAtIsNull(query.knowledgeBaseId)
            ?: return null

        if (knowledgeBase.organizationId != query.organizationId) {
            log.warn { "Knowledge base ${query.knowledgeBaseId} does not belong to organization ${query.organizationId}" }
            return null
        }

        return KnowledgeBaseDto.from(knowledgeBase)
    }
}
