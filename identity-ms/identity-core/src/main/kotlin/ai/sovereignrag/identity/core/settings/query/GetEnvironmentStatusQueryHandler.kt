package ai.sovereignrag.identity.core.settings.query

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.UserService
import ai.sovereignrag.identity.core.settings.dto.EnvironmentStatusResponse
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetEnvironmentStatusQueryHandler(
    private val userService: UserService,
    private val clientRepository: OAuthRegisteredClientRepository
) : Command.Handler<GetEnvironmentStatusQuery, EnvironmentStatusResponse> {

    override fun handle(query: GetEnvironmentStatusQuery): EnvironmentStatusResponse {
        val user = userService.getCurrentUser()
        log.info { "Getting environment status for user: ${user.id}" }

        val merchant = user.merchantId?.toString()?.let { clientRepository.findById(it).orElse(null) }

        val environmentPreference = user.environmentPreference
        val merchantEnvironmentMode = merchant?.environmentMode ?: EnvironmentMode.SANDBOX
        val lastSwitchedAt = user.environmentLastSwitchedAt

        val effectiveEnvironment = determineEffectiveEnvironment(
            userPreference = environmentPreference,
            merchantMode = merchantEnvironmentMode
        )

        return EnvironmentStatusResponse(
            currentEnvironment = effectiveEnvironment,
            environmentPreference = environmentPreference,
            merchantEnvironmentMode = merchantEnvironmentMode,
            lastSwitchedAt = lastSwitchedAt,
            canSwitchToProduction = merchantEnvironmentMode == EnvironmentMode.PRODUCTION
        )
    }

    private fun determineEffectiveEnvironment(userPreference: EnvironmentMode, merchantMode: EnvironmentMode): EnvironmentMode {
        return if (merchantMode == EnvironmentMode.PRODUCTION) userPreference else merchantMode
    }
}
