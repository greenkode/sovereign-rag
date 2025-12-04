package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.GetLanguagesResult
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetLanguagesQueryHandler(
    private val wizardConfigurationService: WizardConfigurationService
) : Command.Handler<GetLanguagesQuery, GetLanguagesResult> {

    override fun handle(query: GetLanguagesQuery): GetLanguagesResult {
        log.debug { "Handling GetLanguagesQuery" }
        return GetLanguagesResult(
            languages = wizardConfigurationService.getEnabledLanguages()
        )
    }
}
