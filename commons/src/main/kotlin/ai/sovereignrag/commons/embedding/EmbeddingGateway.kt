package ai.sovereignrag.commons.embedding

import java.time.Instant
import java.util.UUID

interface EmbeddingGateway {
    fun storeEmbeddings(
        knowledgeBaseId: String,
        sourceId: UUID,
        chunks: List<TextChunk>
    ): List<String>

    fun deleteBySourceId(knowledgeBaseId: String, sourceId: UUID)

    fun deleteByKnowledgeBase(knowledgeBaseId: String)

    fun search(
        knowledgeBaseId: String,
        queryEmbedding: FloatArray,
        maxResults: Int = 10,
        minScore: Double = 0.7
    ): List<EmbeddingMatch>

    fun countBySourceId(knowledgeBaseId: String, sourceId: UUID): Long

    fun countByKnowledgeBase(knowledgeBaseId: String): Long
}

data class TextChunk(
    val id: UUID = UUID.randomUUID(),
    val content: String,
    val embedding: FloatArray,
    val chunkIndex: Int,
    val metadata: ChunkMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TextChunk
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class ChunkMetadata(
    val sourceId: UUID,
    val sourceType: SourceType,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val totalChunks: Int,
    val createdAt: Instant = Instant.now()
)

data class EmbeddingMatch(
    val embeddingId: String,
    val content: String,
    val score: Double,
    val metadata: Map<String, Any>
)

enum class SourceType {
    FILE,
    URL,
    TEXT,
    QA_PAIR,
    RSS_FEED,
    SITEMAP
}

class EmbeddingStoreException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
