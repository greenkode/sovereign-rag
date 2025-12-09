package ai.sovereignrag.knowledgebase.knowledgesource.query

import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.knowledgebase.knowledgesource.dto.KnowledgeSourceSummaryDto
import ai.sovereignrag.knowledgebase.knowledgesource.repository.KnowledgeSourceRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetKnowledgeSourcesQueryHandler(
    private val knowledgeSourceRepository: KnowledgeSourceRepository
) : Command.Handler<GetKnowledgeSourcesQuery, Page<KnowledgeSourceSummaryDto>> {

    override fun handle(query: GetKnowledgeSourcesQuery): Page<KnowledgeSourceSummaryDto> {
        log.debug { "Fetching knowledge sources for knowledge base: ${query.knowledgeBaseId}" }

        val sourcesPage = knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
            knowledgeBaseId = query.knowledgeBaseId,
            status = KnowledgeSourceStatus.DELETED,
            pageable = query.pageable
        )

        return sourcesPage.map { source ->
            KnowledgeSourceSummaryDto(
                id = source.id,
                sourceType = source.sourceType,
                fileName = source.fileName,
                sourceUrl = source.sourceUrl,
                title = source.title,
                fileSize = source.fileSize,
                status = source.status,
                chunkCount = source.chunkCount,
                embeddingCount = source.embeddingCount,
                createdAt = source.createdAt,
                processedAt = source.processedAt
            )
        }
    }
}
