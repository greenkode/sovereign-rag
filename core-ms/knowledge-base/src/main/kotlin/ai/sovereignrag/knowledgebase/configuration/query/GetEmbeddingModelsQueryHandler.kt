package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.GetEmbeddingModelsResult
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetEmbeddingModelsQueryHandler(
    private val wizardConfigurationService: WizardConfigurationService
) : Command.Handler<GetEmbeddingModelsQuery, GetEmbeddingModelsResult> {

    override fun handle(query: GetEmbeddingModelsQuery): GetEmbeddingModelsResult {
        log.debug { "Handling GetEmbeddingModelsQuery with languages: ${query.languageCodes}" }

        val embeddingModels = query.languageCodes
            ?.takeIf { it.isNotEmpty() }
            ?.let { wizardConfigurationService.getEmbeddingModelsByLanguages(it) }
            ?: wizardConfigurationService.getEnabledEmbeddingModels()

        return GetEmbeddingModelsResult(embeddingModels = embeddingModels)
    }
}
