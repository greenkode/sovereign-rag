package ai.sovereignrag.identity.core.settings.query

import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetOrganizationDetailsQueryHandler(
    private val userService: UserService,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository
) : Command.Handler<GetOrganizationDetailsQuery, GetOrganizationDetailsResult> {

    override fun handle(query: GetOrganizationDetailsQuery): GetOrganizationDetailsResult {
        log.info { "Processing GetOrganizationDetailsQuery" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId?.toString()
            ?: throw IllegalStateException("User is not associated with a merchant")

        val client = oAuthRegisteredClientRepository.findByIdWithSettings(merchantId)
            .orElseThrow { IllegalStateException("Merchant not found: $merchantId") }

        val setupCompleted = client.getSetting(OAuthClientSettingName.SETUP_COMPLETED)?.toBoolean() ?: false

        return GetOrganizationDetailsResult(
            id = client.id,
            name = client.clientName,
            plan = client.plan,
            status = client.status,
            environmentMode = client.environmentMode,
            setupCompleted = setupCompleted
        )
    }
}
