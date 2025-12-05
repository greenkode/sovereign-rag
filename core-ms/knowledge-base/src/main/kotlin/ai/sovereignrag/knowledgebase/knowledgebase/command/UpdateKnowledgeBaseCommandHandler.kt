package ai.sovereignrag.knowledgebase.knowledgebase.command

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseDto
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class UpdateKnowledgeBaseCommandHandler(
    private val knowledgeBaseRepository: KnowledgeBaseRepository
) : Command.Handler<UpdateKnowledgeBaseCommand, KnowledgeBaseDto> {

    @Transactional(transactionManager = "masterTransactionManager")
    @CacheEvict(cacheNames = ["knowledge_bases"], key = "#command.knowledgeBaseId")
    override fun handle(command: UpdateKnowledgeBaseCommand): KnowledgeBaseDto {
        log.info { "Updating knowledge base: ${command.knowledgeBaseId}" }

        val knowledgeBase = knowledgeBaseRepository.findByIdAndDeletedAtIsNull(command.knowledgeBaseId)
            ?: throw KnowledgeBaseNotFoundException("Knowledge base not found: ${command.knowledgeBaseId}")

        if (knowledgeBase.organizationId != command.organizationId) {
            throw KnowledgeBaseNotFoundException("Knowledge base not found: ${command.knowledgeBaseId}")
        }

        val updated = knowledgeBase.copy(
            name = command.name ?: knowledgeBase.name,
            description = command.description ?: knowledgeBase.description,
            updatedAt = Instant.now()
        )

        val saved = knowledgeBaseRepository.save(updated)

        log.info { "Knowledge base updated: ${saved.id}" }

        return KnowledgeBaseDto(
            id = saved.id,
            name = saved.name,
            description = saved.description,
            organizationId = saved.organizationId,
            status = saved.status,
            documentCount = 0,
            embeddingCount = 0,
            queryCount = 0,
            maxDocuments = saved.maxDocuments,
            maxEmbeddings = saved.maxEmbeddings,
            maxRequestsPerDay = saved.maxRequestsPerDay,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt,
            lastActiveAt = saved.lastActiveAt
        )
    }
}
