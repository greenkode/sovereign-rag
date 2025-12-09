package ai.sovereignrag.commons.embedding

import java.util.UUID

interface EmbeddingModelGateway {
    fun findById(modelId: String): EmbeddingModelConfig?

    fun findByKnowledgeBase(knowledgeBaseId: UUID): EmbeddingModelConfig?

    fun getDefault(): EmbeddingModelConfig
}

interface EmbeddingModelConfig {
    val id: String
    val name: String
    val modelId: String
    val provider: String
    val dimensions: Int
    val maxTokens: Int
    val baseUrl: String?
}

data class EmbeddingModelNotFoundException(
    val modelId: String
) : RuntimeException("Embedding model not found: $modelId")
