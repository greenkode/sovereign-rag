package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.core.service.MerchantService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class EnableProductionCommandHandler(
    private val userService: UserService,
    private val merchantService: MerchantService
) : Command.Handler<EnableProductionCommand, EnableProductionResult> {

    override fun handle(command: EnableProductionCommand): EnableProductionResult {
        log.info { "Enabling production for merchant with environment: ${command.environmentMode}" }

        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw NotFoundException("User is not associated with a merchant")

        val result = merchantService.updateMerchantEnvironment(
            merchantId = merchantId.toString(),
            environmentMode = command.environmentMode
        )

        log.info { "Successfully enabled ${command.environmentMode} for merchant: $merchantId" }

        return EnableProductionResult(
            merchantId = result.merchantId,
            environmentMode = result.environmentMode,
            affectedUsers = result.affectedUsers,
            success = true,
            message = "Merchant environment updated to ${command.environmentMode}"
        )
    }
}
