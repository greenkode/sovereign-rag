package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.RecommendEmbeddingModelResult
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class RecommendEmbeddingModelQueryHandler(
    private val wizardConfigurationService: WizardConfigurationService
) : Command.Handler<RecommendEmbeddingModelQuery, RecommendEmbeddingModelResult> {

    override fun handle(query: RecommendEmbeddingModelQuery): RecommendEmbeddingModelResult {
        log.debug { "Handling RecommendEmbeddingModelQuery for languages: ${query.languageCodes}" }

        val languages = query.languageCodes

        if (languages.isEmpty()) {
            val allModels = wizardConfigurationService.getEnabledEmbeddingModels()
            return RecommendEmbeddingModelResult(
                recommended = allModels.firstOrNull(),
                alternatives = allModels.drop(1)
            )
        }

        val optimizedModels = wizardConfigurationService.getOptimizedEmbeddingModels(languages)
        val supportedModels = wizardConfigurationService.getEmbeddingModelsByLanguages(languages)

        val recommended = optimizedModels.firstOrNull()
            ?: supportedModels.firstOrNull()

        val alternatives = (optimizedModels + supportedModels)
            .distinctBy { it.id }
            .filter { it.id != recommended?.id }

        return RecommendEmbeddingModelResult(
            recommended = recommended,
            alternatives = alternatives
        )
    }
}
