package ai.sovereignrag.knowledgebase.configuration.dto

import ai.sovereignrag.knowledgebase.configuration.domain.EmbeddingModel
import ai.sovereignrag.knowledgebase.configuration.domain.Language
import ai.sovereignrag.knowledgebase.configuration.domain.Region

data class RegionDto(
    val code: String,
    val name: String,
    val continent: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val enabled: Boolean
) {
    companion object {
        fun from(entity: Region) = RegionDto(
            code = entity.code,
            name = entity.name,
            continent = entity.continent,
            city = entity.city,
            country = entity.country,
            countryCode = entity.countryCode,
            flag = entity.flag,
            enabled = entity.enabled
        )
    }
}

data class LanguageDto(
    val code: String,
    val name: String,
    val nativeName: String,
    val enabled: Boolean
) {
    companion object {
        fun from(entity: Language) = LanguageDto(
            code = entity.code,
            name = entity.name,
            nativeName = entity.nativeName,
            enabled = entity.enabled
        )
    }
}

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
) {
    companion object {
        fun from(entity: EmbeddingModel) = EmbeddingModelDto(
            id = entity.id,
            name = entity.name,
            modelId = entity.modelId,
            description = entity.description,
            provider = entity.provider,
            dimensions = entity.dimensions,
            maxTokens = entity.maxTokens,
            supportedLanguages = entity.supportedLanguages,
            optimizedFor = entity.optimizedFor,
            enabled = entity.enabled
        )
    }
}

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
