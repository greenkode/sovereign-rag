package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class UpdateAlertsCommandHandler(
    private val userService: UserService,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val objectMapper: ObjectMapper,
    private val cacheEvictionService: CacheEvictionService
) : Command.Handler<UpdateAlertsCommand, UpdateAlertsResult> {

    override fun handle(command: UpdateAlertsCommand): UpdateAlertsResult {

        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw NotFoundException("User is not associated with a merchant")

        val client = oAuthRegisteredClientRepository.findById(merchantId.toString())
            .orElseThrow { NotFoundException("Merchant client not found") }

        val existingSettings = if (client.clientSettings.isNotBlank()) {
            objectMapper.readValue(client.clientSettings, Map::class.java) as MutableMap<String, Any>
        } else {
            mutableMapOf()
        }

        existingSettings["failureLimit"] = command.failureLimit.toString()
        existingSettings["lowBalance"] = command.lowBalance.toString()

        client.clientSettings = objectMapper.writeValueAsString(existingSettings)
        oAuthRegisteredClientRepository.save(client)

        log.info { "Updated alert settings for merchant: ${client.clientId}" }

        // Evict the MERCHANT_DETAILS cache since merchant settings have changed
        cacheEvictionService.evictMerchantCaches(merchantId.toString())

        return UpdateAlertsResult(
            success = true,
            message = "Alert settings updated successfully"
        )
    }
}