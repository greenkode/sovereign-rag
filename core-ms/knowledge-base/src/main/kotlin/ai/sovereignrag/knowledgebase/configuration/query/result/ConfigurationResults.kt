package ai.sovereignrag.knowledgebase.configuration.query.result

import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import ai.sovereignrag.knowledgebase.configuration.dto.LanguageDto
import ai.sovereignrag.knowledgebase.configuration.dto.RegionDto

data class GetWizardConfigurationResult(
    val regions: List<RegionDto>,
    val languages: List<LanguageDto>,
    val embeddingModels: List<EmbeddingModelDto>
)

data class GetRegionsResult(
    val regions: List<RegionDto>
)

data class GetLanguagesResult(
    val languages: List<LanguageDto>
)

data class GetEmbeddingModelsResult(
    val embeddingModels: List<EmbeddingModelDto>
)

data class RecommendEmbeddingModelResult(
    val recommended: EmbeddingModelDto?,
    val alternatives: List<EmbeddingModelDto>
)
