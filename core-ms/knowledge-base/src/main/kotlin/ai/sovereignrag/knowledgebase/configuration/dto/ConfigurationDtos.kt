package ai.sovereignrag.knowledgebase.configuration.dto

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.knowledgebase.configuration.domain.LlmPrivacyLevel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType
import java.math.BigDecimal

data class RegionDto(
    val code: String,
    val name: String,
    val continent: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val enabled: Boolean
)

data class LanguageDto(
    val code: String,
    val name: String,
    val nativeName: String,
    val enabled: Boolean
)

data class EmbeddingModelDto(
    val id: String,
    val name: String,
    val modelId: String,
    val description: String,
    val provider: String,
    val dimensions: Int,
    val maxTokens: Int,
    val supportedLanguages: Set<String>,
    val optimizedFor: Set<String>,
    val enabled: Boolean,
    val baseUrl: String? = null
)

data class WizardConfigurationResponse(
    val regions: List<RegionDto>,
    val languages: List<LanguageDto>,
    val embeddingModels: List<EmbeddingModelDto>
)

data class EmbeddingModelRecommendationRequest(
    val languageCodes: Set<String>
)

data class EmbeddingModelRecommendationResponse(
    val recommended: EmbeddingModelDto?,
    val alternatives: List<EmbeddingModelDto>
)

data class LlmModelDto(
    val id: String,
    val name: String,
    val modelId: String,
    val description: String,
    val provider: String,
    val providerType: LlmProviderType,
    val maxTokens: Int,
    val contextWindow: Int,
    val supportsStreaming: Boolean,
    val supportsFunctionCalling: Boolean,
    val privacyLevel: LlmPrivacyLevel,
    val minTier: SubscriptionTier,
    val costPer1kInputTokens: BigDecimal?,
    val costPer1kOutputTokens: BigDecimal?,
    val baseUrl: String?,
    val enabled: Boolean,
    val isDefault: Boolean,
    val capabilities: Set<String>
)

data class LlmModelRecommendationRequest(
    val requiresPrivacy: Boolean = true,
    val requiredCapabilities: Set<String> = emptySet()
)

data class LlmModelRecommendationResponse(
    val recommended: LlmModelDto?,
    val alternatives: List<LlmModelDto>
)
