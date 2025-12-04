package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.GetWizardConfigurationResult
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetWizardConfigurationQueryHandler(
    private val wizardConfigurationService: WizardConfigurationService
) : Command.Handler<GetWizardConfigurationQuery, GetWizardConfigurationResult> {

    override fun handle(query: GetWizardConfigurationQuery): GetWizardConfigurationResult {
        log.debug { "Handling GetWizardConfigurationQuery" }

        val regions = wizardConfigurationService.getEnabledRegions()
        val languages = wizardConfigurationService.getEnabledLanguages()
        val embeddingModels = wizardConfigurationService.getEnabledEmbeddingModels()

        return GetWizardConfigurationResult(
            regions = regions,
            languages = languages,
            embeddingModels = embeddingModels
        )
    }
}
