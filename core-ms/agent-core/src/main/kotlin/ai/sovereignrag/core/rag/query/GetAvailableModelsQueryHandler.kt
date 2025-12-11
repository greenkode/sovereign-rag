package ai.sovereignrag.core.rag.query

import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.core.llm.LlmModelSelectionService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetAvailableModelsQueryHandler(
    private val llmModelSelectionService: LlmModelSelectionService,
    private val licenseConfiguration: LicenseConfiguration
) : Command.Handler<GetAvailableModelsQuery, AvailableModelsResult> {

    override fun handle(query: GetAvailableModelsQuery): AvailableModelsResult {
        val tier = licenseConfiguration.getLicenseInfo().tier
        log.debug { "Getting available models for tier $tier (privacyOnly: ${query.privacyCompliantOnly})" }

        val models = if (query.privacyCompliantOnly) {
            llmModelSelectionService.getPrivacyCompliantModels(tier)
        } else {
            llmModelSelectionService.getAvailableModels(tier)
        }

        val defaultModel = if (query.privacyCompliantOnly) {
            models.firstOrNull() ?: llmModelSelectionService.getDefaultModel()
        } else {
            llmModelSelectionService.getDefaultModel()
        }

        return AvailableModelsResult(
            models = models.map { it.toDto() },
            defaultModelId = defaultModel.id
        )
    }
}
