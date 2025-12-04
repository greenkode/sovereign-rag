package ai.sovereignrag.knowledgebase.configuration.dto

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
    val enabled: Boolean
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
