package ai.sovereignrag.knowledgebase.configuration.domain

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.knowledgebase.configuration.dto.LlmModelDto
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

enum class LlmProviderType {
    LOCAL,
    CLOUD
}

enum class LlmPrivacyLevel {
    MAXIMUM,
    STANDARD
}

@Entity
data class LlmModel(
    @Id
    val id: String,
    val name: String,
    val modelId: String,
    val description: String,
    val provider: String,

    @Enumerated(EnumType.STRING)
    val providerType: LlmProviderType,

    val maxTokens: Int,
    val contextWindow: Int,
    val supportsStreaming: Boolean = true,
    val supportsFunctionCalling: Boolean = false,

    @Enumerated(EnumType.STRING)
    val privacyLevel: LlmPrivacyLevel = LlmPrivacyLevel.STANDARD,

    @Enumerated(EnumType.STRING)
    val minTier: SubscriptionTier = SubscriptionTier.TRIAL,

    val costPer1kInputTokens: BigDecimal? = null,
    val costPer1kOutputTokens: BigDecimal? = null,
    val baseUrl: String? = null,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "llm_model_capability",
        joinColumns = [JoinColumn(name = "model_id")]
    )
    @Column(name = "capability")
    val capabilities: Set<String> = emptySet()
) : Serializable {

    fun toDto() = LlmModelDto(
        id = id,
        name = name,
        modelId = modelId,
        description = description,
        provider = provider,
        providerType = providerType,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        supportsStreaming = supportsStreaming,
        supportsFunctionCalling = supportsFunctionCalling,
        privacyLevel = privacyLevel,
        minTier = minTier,
        costPer1kInputTokens = costPer1kInputTokens,
        costPer1kOutputTokens = costPer1kOutputTokens,
        baseUrl = baseUrl,
        enabled = enabled,
        isDefault = isDefault,
        capabilities = capabilities
    )

    fun isAccessibleByTier(tier: SubscriptionTier): Boolean = tier.priority >= minTier.priority

    fun isPrivacyCompliant(): Boolean = providerType == LlmProviderType.LOCAL
}
