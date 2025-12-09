package ai.sovereignrag.ingestion.core.embedding

import ai.sovereignrag.commons.embedding.ChunkMetadata
import ai.sovereignrag.commons.embedding.EmbeddingGateway
import ai.sovereignrag.commons.embedding.EmbeddingMatch
import ai.sovereignrag.commons.embedding.TextChunk
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Component
class PgVectorEmbeddingGateway(
    private val ingestionProperties: IngestionProperties
) : EmbeddingGateway {

    private val embeddingStores = ConcurrentHashMap<String, EmbeddingStore<TextSegment>>()

    override fun storeEmbeddings(
        knowledgeBaseId: String,
        sourceId: UUID,
        chunks: List<TextChunk>
    ): List<String> {
        val store = getOrCreateStore(knowledgeBaseId)

        val embeddings = chunks.map { chunk ->
            Embedding.from(chunk.embedding)
        }

        val segments = chunks.map { chunk ->
            TextSegment.from(
                chunk.content,
                buildMetadata(chunk.metadata, sourceId, chunk.chunkIndex)
            )
        }

        val ids = store.addAll(embeddings, segments)

        log.info { "Stored ${ids.size} embeddings for knowledge base $knowledgeBaseId, source $sourceId" }

        return ids
    }

    override fun deleteBySourceId(knowledgeBaseId: String, sourceId: UUID) {
        val store = getOrCreateStore(knowledgeBaseId)
        store.removeAll(metadataKey("source_id").isEqualTo(sourceId.toString()))
        log.info { "Deleted embeddings for source $sourceId from knowledge base $knowledgeBaseId" }
    }

    override fun deleteByKnowledgeBase(knowledgeBaseId: String) {
        val store = getOrCreateStore(knowledgeBaseId)
        store.removeAll()
        embeddingStores.remove(knowledgeBaseId)
        log.info { "Deleted all embeddings for knowledge base $knowledgeBaseId" }
    }

    override fun search(
        knowledgeBaseId: String,
        queryEmbedding: FloatArray,
        maxResults: Int,
        minScore: Double
    ): List<EmbeddingMatch> {
        val store = getOrCreateStore(knowledgeBaseId)

        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(Embedding.from(queryEmbedding))
            .maxResults(maxResults)
            .minScore(minScore)
            .build()

        val response = store.search(searchRequest)

        return response.matches().map { match ->
            EmbeddingMatch(
                embeddingId = match.embeddingId(),
                content = match.embedded()?.text() ?: "",
                score = match.score(),
                metadata = match.embedded()?.metadata()?.toMap() ?: emptyMap()
            )
        }
    }

    override fun countBySourceId(knowledgeBaseId: String, sourceId: UUID): Long {
        val store = getOrCreateStore(knowledgeBaseId)
        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(Embedding.from(FloatArray(ingestionProperties.embedding.dimension)))
            .maxResults(Int.MAX_VALUE)
            .minScore(0.0)
            .filter(metadataKey("source_id").isEqualTo(sourceId.toString()))
            .build()

        return store.search(searchRequest).matches().size.toLong()
    }

    override fun countByKnowledgeBase(knowledgeBaseId: String): Long {
        val store = getOrCreateStore(knowledgeBaseId)
        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(Embedding.from(FloatArray(ingestionProperties.embedding.dimension)))
            .maxResults(Int.MAX_VALUE)
            .minScore(0.0)
            .build()

        return store.search(searchRequest).matches().size.toLong()
    }

    private fun getOrCreateStore(knowledgeBaseId: String): EmbeddingStore<TextSegment> {
        return embeddingStores.computeIfAbsent(knowledgeBaseId) { kbId ->
            val tableName = getTableName(kbId)
            val pgConfig = ingestionProperties.embedding.pgvector

            PgVectorEmbeddingStore.builder()
                .host(pgConfig.host)
                .port(pgConfig.port)
                .database(pgConfig.database)
                .user(pgConfig.user)
                .password(pgConfig.password)
                .table(tableName)
                .dimension(ingestionProperties.embedding.dimension)
                .createTable(true)
                .useIndex(true)
                .indexListSize(100)
                .build().also {
                    log.info { "Created PgVectorEmbeddingStore for knowledge base $kbId with table $tableName" }
                }
        }
    }

    private fun getTableName(knowledgeBaseId: String): String {
        val sanitizedId = knowledgeBaseId.replace("-", "_").lowercase()
        return "${ingestionProperties.embedding.tablePrefix}${sanitizedId}${ingestionProperties.embedding.tableSuffix}"
    }

    private fun buildMetadata(chunkMetadata: ChunkMetadata, sourceId: UUID, chunkIndex: Int): Metadata {
        return Metadata.from(
            mapOf(
                "source_id" to sourceId.toString(),
                "source_type" to chunkMetadata.sourceType.name,
                "file_name" to (chunkMetadata.fileName ?: ""),
                "source_url" to (chunkMetadata.sourceUrl ?: ""),
                "title" to (chunkMetadata.title ?: ""),
                "chunk_index" to chunkIndex.toString(),
                "total_chunks" to chunkMetadata.totalChunks.toString(),
                "created_at" to chunkMetadata.createdAt.toString()
            )
        )
    }
}
