package ai.sovereignrag.core.rag.retrieval

import ai.sovereignrag.commons.embedding.EmbeddingModelGateway
import ai.sovereignrag.core.embedding.EmbeddingModelFactory
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Component
class KnowledgeBaseRetrieverFactory(
    private val embeddingModelGateway: EmbeddingModelGateway,
    private val embeddingModelFactory: EmbeddingModelFactory,
    private val vectorStoreConnectionProvider: VectorStoreConnectionProvider
) {
    private val embeddingStoreCache = ConcurrentHashMap<String, EmbeddingStore<TextSegment>>()
    private val embeddingModelCache = ConcurrentHashMap<String, EmbeddingModel>()
    private val defaultDimension = 768

    fun createRetriever(
        knowledgeBaseId: String,
        maxResults: Int = 5,
        minScore: Double = 0.7
    ): ContentRetriever {
        log.debug { "Creating content retriever for KB: $knowledgeBaseId" }

        val embeddingStore = getOrCreateEmbeddingStore(knowledgeBaseId)
        val embeddingModel = getOrCreateEmbeddingModel(knowledgeBaseId)

        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .minScore(minScore)
            .build()
    }

    private fun getOrCreateEmbeddingStore(knowledgeBaseId: String): EmbeddingStore<TextSegment> {
        return embeddingStoreCache.computeIfAbsent(knowledgeBaseId) { kbId ->
            val connectionInfo = vectorStoreConnectionProvider.getConnectionInfo(kbId)
            val dimension = getEmbeddingDimension(kbId)

            log.info { "Creating PgVectorEmbeddingStore for KB $kbId (dimension: $dimension)" }

            PgVectorEmbeddingStore.builder()
                .host(connectionInfo.host)
                .port(connectionInfo.port)
                .database(connectionInfo.database)
                .user(connectionInfo.username)
                .password(connectionInfo.password)
                .table("${connectionInfo.schema}.${connectionInfo.tableName}")
                .dimension(dimension)
                .build()
        }
    }

    private fun getOrCreateEmbeddingModel(knowledgeBaseId: String): EmbeddingModel {
        return embeddingModelCache.computeIfAbsent(knowledgeBaseId) { kbId ->
            val modelConfig = embeddingModelGateway.findByKnowledgeBase(UUID.fromString(kbId))
                ?: embeddingModelGateway.getDefault()

            log.info { "Creating embedding model for KB $kbId: ${modelConfig.name} (${modelConfig.provider})" }
            embeddingModelFactory.createEmbeddingModel(modelConfig)
        }
    }

    private fun getEmbeddingDimension(knowledgeBaseId: String): Int {
        val modelConfig = embeddingModelGateway.findByKnowledgeBase(UUID.fromString(knowledgeBaseId))
            ?: embeddingModelGateway.getDefault()
        return modelConfig.dimensions.takeIf { it > 0 } ?: defaultDimension
    }

    fun evictCache(knowledgeBaseId: String) {
        embeddingStoreCache.remove(knowledgeBaseId)
        embeddingModelCache.remove(knowledgeBaseId)
        log.info { "Evicted retriever cache for KB: $knowledgeBaseId" }
    }
}
