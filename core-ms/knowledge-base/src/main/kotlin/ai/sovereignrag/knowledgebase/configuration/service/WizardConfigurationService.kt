package ai.sovereignrag.knowledgebase.configuration.service

import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import ai.sovereignrag.knowledgebase.configuration.dto.LanguageDto
import ai.sovereignrag.knowledgebase.configuration.dto.RegionDto
import ai.sovereignrag.knowledgebase.configuration.repository.EmbeddingModelRepository
import ai.sovereignrag.knowledgebase.configuration.repository.LanguageRepository
import ai.sovereignrag.knowledgebase.configuration.repository.RegionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class WizardConfigurationService(
    private val regionRepository: RegionRepository,
    private val languageRepository: LanguageRepository,
    private val embeddingModelRepository: EmbeddingModelRepository
) {

    fun getEnabledRegions(): List<RegionDto> {
        log.debug { "Fetching enabled regions" }
        return regionRepository.findByEnabledTrueOrderBySortOrder()
            .map { it.toDto() }
    }

    fun getEnabledLanguages(): List<LanguageDto> {
        log.debug { "Fetching enabled languages" }
        return languageRepository.findByEnabledTrueOrderBySortOrder()
            .map { it.toDto() }
    }

    fun getEnabledEmbeddingModels(): List<EmbeddingModelDto> {
        log.debug { "Fetching enabled embedding models" }
        return embeddingModelRepository.findByEnabledTrueOrderBySortOrder()
            .map { it.toDto() }
    }

    fun getEmbeddingModelsByLanguages(languageCodes: Set<String>): List<EmbeddingModelDto> {
        log.debug { "Fetching embedding models for languages: $languageCodes" }
        return embeddingModelRepository.findByLanguagesSupported(languageCodes)
            .map { it.toDto() }
    }

    fun getOptimizedEmbeddingModels(languageCodes: Set<String>): List<EmbeddingModelDto> {
        log.debug { "Fetching optimized embedding models for languages: $languageCodes" }
        return embeddingModelRepository.findByLanguagesOptimized(languageCodes)
            .map { it.toDto() }
    }
}
