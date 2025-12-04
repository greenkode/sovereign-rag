package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.GetRegionsResult
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetRegionsQueryHandler(
    private val wizardConfigurationService: WizardConfigurationService
) : Command.Handler<GetRegionsQuery, GetRegionsResult> {

    override fun handle(query: GetRegionsQuery): GetRegionsResult {
        log.debug { "Handling GetRegionsQuery" }
        return GetRegionsResult(
            regions = wizardConfigurationService.getEnabledRegions()
        )
    }
}
