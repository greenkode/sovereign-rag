package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ResetKeysCommandHandler(
    private val userService: UserService,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder
) : Command.Handler<ResetKeysCommand, ResetKeysResult> {

    override fun handle(command: ResetKeysCommand): ResetKeysResult {
        log.info { "Processing ResetKeysCommand" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId
            ?: throw RecordNotFoundException("User is not associated with a merchant")

        val client = oAuthRegisteredClientRepository.findById(merchantId.toString())
            .orElseThrow { RecordNotFoundException("Merchant client not found") }

        val targetEnvironment = command.environment ?: user.environmentPreference

        if (client.environmentMode == EnvironmentMode.SANDBOX && targetEnvironment == EnvironmentMode.PRODUCTION) {
            throw InvalidRequestException("Cannot reset production keys for a merchant in sandbox mode")
        }

        if (user.environmentPreference == EnvironmentMode.SANDBOX && targetEnvironment == EnvironmentMode.PRODUCTION) {
            throw InvalidRequestException("Cannot reset production keys while in sandbox environment")
        }

        val newClientSecret = RandomStringUtils.secure().nextAlphanumeric(30)
        val encodedSecret = passwordEncoder.encode(newClientSecret)

        when (targetEnvironment) {
            EnvironmentMode.SANDBOX -> {
                client.sandboxClientSecret = encodedSecret
                log.info { "Reset sandbox client secret for merchant: ${client.clientId}" }
            }
            EnvironmentMode.PRODUCTION -> {
                client.productionClientSecret = encodedSecret
                log.info { "Reset production client secret for merchant: ${client.clientId}" }
            }
        }

        oAuthRegisteredClientRepository.save(client)

        return ResetKeysResult(
            clientId = client.clientId,
            clientSecret = newClientSecret,
            environment = targetEnvironment,
            success = true,
            message = "Client keys reset successfully for ${targetEnvironment.name} environment"
        )
    }
}