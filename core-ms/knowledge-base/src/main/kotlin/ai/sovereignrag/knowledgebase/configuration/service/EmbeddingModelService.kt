package ai.sovereignrag.knowledgebase.configuration.service

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.commons.embedding.EmbeddingModelGateway
import ai.sovereignrag.commons.embedding.EmbeddingModelNotFoundException
import ai.sovereignrag.knowledgebase.configuration.repository.EmbeddingModelRepository
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true, transactionManager = "masterTransactionManager")
class EmbeddingModelService(
    private val embeddingModelRepository: EmbeddingModelRepository,
    private val knowledgeBaseRepository: KnowledgeBaseRepository
) : EmbeddingModelGateway {

    override fun findById(modelId: String): EmbeddingModelConfig? {
        return embeddingModelRepository.findById(modelId).orElse(null)
    }

    override fun findByKnowledgeBase(knowledgeBaseId: UUID): EmbeddingModelConfig? {
        val knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId.toString()).orElse(null)
            ?: return null

        val embeddingModelId = knowledgeBase.embeddingModelId
            ?: return getDefault()

        return findById(embeddingModelId) ?: getDefault()
    }

    override fun getDefault(): EmbeddingModelConfig {
        val models = embeddingModelRepository.findByEnabledTrueOrderBySortOrder()
        return models.firstOrNull()
            ?: throw EmbeddingModelNotFoundException("default")
    }
}
