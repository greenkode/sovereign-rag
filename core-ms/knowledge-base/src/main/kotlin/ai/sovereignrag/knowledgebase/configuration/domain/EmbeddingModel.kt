package ai.sovereignrag.knowledgebase.configuration.domain

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import java.io.Serializable
import java.time.Instant

@Entity
data class EmbeddingModel(
    @Id
    override val id: String,
    override val name: String,
    override val modelId: String,
    val description: String,
    override val provider: String,
    override val dimensions: Int,
    override val maxTokens: Int,
    override val baseUrl: String? = null,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "embedding_model_language",
        joinColumns = [JoinColumn(name = "model_id")]
    )
    @Column(name = "language_code")
    val supportedLanguages: Set<String> = emptySet(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "embedding_model_optimized_language",
        joinColumns = [JoinColumn(name = "model_id")]
    )
    @Column(name = "language_code")
    val optimizedFor: Set<String> = emptySet()
) : EmbeddingModelConfig, Serializable {
    fun toDto() = EmbeddingModelDto(
        id = id,
        name = name,
        modelId = modelId,
        description = description,
        provider = provider,
        dimensions = dimensions,
        maxTokens = maxTokens,
        supportedLanguages = supportedLanguages,
        optimizedFor = optimizedFor,
        enabled = enabled,
        baseUrl = baseUrl
    )
}
