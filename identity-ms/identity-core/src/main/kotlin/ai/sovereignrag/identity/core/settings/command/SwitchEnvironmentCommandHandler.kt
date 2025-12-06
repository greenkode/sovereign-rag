package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CustomUserDetails
import ai.sovereignrag.identity.core.service.UserService
import ai.sovereignrag.identity.core.settings.dto.EnvironmentStatusResponse
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@Transactional
class SwitchEnvironmentCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val clientRepository: OAuthRegisteredClientRepository,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val request: HttpServletRequest
) : Command.Handler<SwitchEnvironmentCommand, EnvironmentStatusResponse> {

    override fun handle(command: SwitchEnvironmentCommand): EnvironmentStatusResponse {
        val user = userService.getCurrentUser()
        log.info { "Switching environment for user: ${user.id} to: ${command.environment}" }

        val merchant = user.merchantId?.let { clientRepository.findById(it).orElse(null) }
        val merchantEnvironmentMode = merchant?.environmentMode ?: EnvironmentMode.SANDBOX

        if (command.environment == EnvironmentMode.PRODUCTION && merchantEnvironmentMode == EnvironmentMode.SANDBOX) {
            throw IllegalArgumentException(
                "Cannot switch to PRODUCTION environment. Merchant is in SANDBOX mode. " +
                "Contact super admin to upgrade merchant to PRODUCTION."
            )
        }

        user.environmentPreference = if(merchantEnvironmentMode == EnvironmentMode.SANDBOX) EnvironmentMode.SANDBOX else command.environment
        user.environmentLastSwitchedAt = Instant.now()
        userRepository.save(user)

        val userDetails = CustomUserDetails(user)
        val newAccessToken = jwtTokenService.generateToken(user, userDetails)

        val ipAddress = request.remoteAddr
        val userAgent = request.getHeader("User-Agent")
        val newRefreshToken = refreshTokenService.createRefreshToken(
            user = user,
            ipAddress = ipAddress,
            userAgent = userAgent,
            deviceFingerprint = null
        )

        log.info { "Successfully switched environment for user: ${user.id} to: ${command.environment} and generated new tokens" }

        return EnvironmentStatusResponse(
            currentEnvironment = user.environmentPreference,
            environmentPreference = command.environment,
            merchantEnvironmentMode = merchantEnvironmentMode,
            lastSwitchedAt = user.environmentLastSwitchedAt,
            canSwitchToProduction = merchantEnvironmentMode == EnvironmentMode.PRODUCTION,
            requiresReAuthentication = false,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtTokenService.getTokenExpirySeconds()
        )
    }
}
