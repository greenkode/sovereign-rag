package ai.sovereignrag.knowledgebase.knowledgebase.query

import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseDto
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
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

        return KnowledgeBaseDto(
            id = knowledgeBase.id,
            name = knowledgeBase.name,
            description = knowledgeBase.description,
            organizationId = knowledgeBase.organizationId,
            oauthClientId = knowledgeBase.oauthClientId,
            regionCode = knowledgeBase.regionCode,
            status = knowledgeBase.status,
            knowledgeSourceCount = 0,
            embeddingCount = 0,
            queryCount = 0,
            maxKnowledgeSources = knowledgeBase.maxKnowledgeSources,
            maxEmbeddings = knowledgeBase.maxEmbeddings,
            maxRequestsPerDay = knowledgeBase.maxRequestsPerDay,
            embeddingModelId = knowledgeBase.embeddingModelId,
            llmModelId = knowledgeBase.llmModelId,
            requiresEncryption = knowledgeBase.requiresEncryption,
            createdAt = knowledgeBase.createdAt,
            updatedAt = knowledgeBase.updatedAt,
            lastActiveAt = knowledgeBase.lastActiveAt
        )
    }
}
