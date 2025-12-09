package ai.sovereignrag.knowledgebase.knowledgesource.query

import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.knowledgebase.knowledgesource.dto.KnowledgeSourceDto
import ai.sovereignrag.knowledgebase.knowledgesource.repository.KnowledgeSourceRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetKnowledgeSourceQueryHandler(
    private val knowledgeSourceRepository: KnowledgeSourceRepository
) : Command.Handler<GetKnowledgeSourceQuery, KnowledgeSourceDto?> {

    override fun handle(query: GetKnowledgeSourceQuery): KnowledgeSourceDto? {
        log.debug { "Fetching knowledge source: ${query.sourceId} for knowledge base: ${query.knowledgeBaseId}" }

        return knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
            id = query.sourceId,
            knowledgeBaseId = query.knowledgeBaseId,
            status = KnowledgeSourceStatus.DELETED
        )?.let { source ->
            KnowledgeSourceDto(
                id = source.id,
                knowledgeBaseId = source.knowledgeBaseId,
                sourceType = source.sourceType,
                fileName = source.fileName,
                sourceUrl = source.sourceUrl,
                title = source.title,
                mimeType = source.mimeType,
                fileSize = source.fileSize,
                status = source.status,
                errorMessage = source.errorMessage,
                chunkCount = source.chunkCount,
                embeddingCount = source.embeddingCount,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt,
                processedAt = source.processedAt
            )
        }
    }
}
