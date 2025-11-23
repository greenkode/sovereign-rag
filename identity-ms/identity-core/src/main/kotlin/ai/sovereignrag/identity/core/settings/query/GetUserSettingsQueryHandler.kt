package ai.sovereignrag.identity.core.settings.query

import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetUserSettingsQueryHandler(
    private val userService: UserService,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository
) : Command.Handler<GetUserSettingsQuery, GetUserSettingsResult> {

    override fun handle(query: GetUserSettingsQuery): GetUserSettingsResult {
        log.info { "Processing GetUserSettingsQuery" }

        val user = userService.getCurrentUser()
        val roles = userService.getCurrentUserRoles()

        val merchantName = user.merchantId?.let { merchantId ->
            oAuthRegisteredClientRepository.findById(merchantId.toString())
                .map { it.clientName }
                .orElse(null)
        }

        return GetUserSettingsResult(
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            roles = roles,
            merchantName = merchantName
        )
    }
}