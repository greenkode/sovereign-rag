package ai.sovereignrag.knowledgebase.configuration.domain

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
    val id: String,
    val name: String,
    val modelId: String,
    val description: String,
    val provider: String,
    val dimensions: Int,
    val maxTokens: Int,
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
) : Serializable
