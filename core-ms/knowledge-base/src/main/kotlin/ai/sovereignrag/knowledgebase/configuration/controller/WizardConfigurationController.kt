package ai.sovereignrag.knowledgebase.configuration.controller

import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelRecommendationRequest
import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelRecommendationResponse
import ai.sovereignrag.knowledgebase.configuration.dto.LanguageDto
import ai.sovereignrag.knowledgebase.configuration.dto.RegionDto
import ai.sovereignrag.knowledgebase.configuration.dto.WizardConfigurationResponse
import ai.sovereignrag.knowledgebase.configuration.query.GetEmbeddingModelsQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetLanguagesQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetRegionsQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetWizardConfigurationQuery
import ai.sovereignrag.knowledgebase.configuration.query.RecommendEmbeddingModelQuery
import ai.sovereignrag.commons.security.IsMerchant
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/knowledge-bases/configuration")
class WizardConfigurationController(
    private val pipeline: Pipeline
) {

    @GetMapping
    @IsMerchant
    fun getWizardConfiguration(): ResponseEntity<WizardConfigurationResponse> {
        log.info { "Fetching wizard configuration" }

        val result = pipeline.send(GetWizardConfigurationQuery())

        return ResponseEntity.ok(
            WizardConfigurationResponse(
                regions = result.regions,
                languages = result.languages,
                embeddingModels = result.embeddingModels
            )
        )
    }

    @GetMapping("/regions")
    @IsMerchant
    fun getRegions(): ResponseEntity<List<RegionDto>> {
        log.info { "Fetching regions" }
        val result = pipeline.send(GetRegionsQuery())
        return ResponseEntity.ok(result.regions)
    }

    @GetMapping("/languages")
    @IsMerchant
    fun getLanguages(): ResponseEntity<List<LanguageDto>> {
        log.info { "Fetching languages" }
        val result = pipeline.send(GetLanguagesQuery())
        return ResponseEntity.ok(result.languages)
    }

    @GetMapping("/embedding-models")
    @IsMerchant
    fun getEmbeddingModels(
        @RequestParam(required = false) languages: Set<String>?
    ): ResponseEntity<List<EmbeddingModelDto>> {
        log.info { "Fetching embedding models, filter by languages: $languages" }
        val result = pipeline.send(GetEmbeddingModelsQuery(languageCodes = languages))
        return ResponseEntity.ok(result.embeddingModels)
    }

    @PostMapping("/embedding-models/recommend")
    @IsMerchant
    fun recommendEmbeddingModel(
        @RequestBody request: EmbeddingModelRecommendationRequest
    ): ResponseEntity<EmbeddingModelRecommendationResponse> {
        log.info { "Recommending embedding model for languages: ${request.languageCodes}" }

        val result = pipeline.send(RecommendEmbeddingModelQuery(languageCodes = request.languageCodes))

        return ResponseEntity.ok(
            EmbeddingModelRecommendationResponse(
                recommended = result.recommended,
                alternatives = result.alternatives
            )
        )
    }
}
