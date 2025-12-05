package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class UpdateAlertsCommandHandler(
    private val userService: UserService,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val cacheEvictionService: CacheEvictionService
) : Command.Handler<UpdateAlertsCommand, UpdateAlertsResult> {

    override fun handle(command: UpdateAlertsCommand): UpdateAlertsResult {

        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw RecordNotFoundException("User is not associated with a merchant")

        val client = oAuthRegisteredClientRepository.findById(merchantId.toString())
            .orElseThrow { RecordNotFoundException("Merchant client not found") }

        client.addSetting(OAuthClientSettingName.FAILURE_LIMIT, command.failureLimit.toString())
        client.addSetting(OAuthClientSettingName.LOW_BALANCE, command.lowBalance.toString())

        oAuthRegisteredClientRepository.save(client)

        log.info { "Updated alert settings for merchant: ${client.clientId}" }

        cacheEvictionService.evictMerchantCaches(merchantId.toString())

        return UpdateAlertsResult(
            success = true,
            message = "Alert settings updated successfully"
        )
    }
}
