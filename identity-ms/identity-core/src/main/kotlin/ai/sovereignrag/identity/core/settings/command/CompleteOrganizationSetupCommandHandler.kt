package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.commons.user.dto.PhoneNumber
import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CompleteOrganizationSetupCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService
) : Command.Handler<CompleteOrganizationSetupCommand, CompleteOrganizationSetupResult> {

    override fun handle(command: CompleteOrganizationSetupCommand): CompleteOrganizationSetupResult {
        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        val client = oAuthRegisteredClientRepository.findById(merchantId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("settings.error.merchant_not_found")) }

        if (client.getSetting(OAuthClientSettingName.SETUP_COMPLETED) == "true") {
            throw InvalidRequestException(messageService.getMessage("settings.error.setup_already_completed"))
        }

        if (!command.termsAccepted) {
            throw InvalidRequestException(messageService.getMessage("settings.error.terms_required"))
        }

        client.clientName = command.companyName
        client.status = OrganizationStatus.ACTIVE
        client.addSetting(OAuthClientSettingName.INTENDED_PURPOSE, command.intendedPurpose.name)
        client.addSetting(OAuthClientSettingName.COMPANY_SIZE, command.companySize.name)
        client.addSetting(OAuthClientSettingName.ROLE_IN_COMPANY, command.roleInCompany.name)
        client.addSetting(OAuthClientSettingName.COUNTRY, command.country)
        client.addSetting(OAuthClientSettingName.PHONE_NUMBER, command.phoneNumber)
        command.website?.takeIf { it.isNotBlank() && it != "https://" }?.let {
            client.addSetting(OAuthClientSettingName.WEBSITE, it)
        }
        client.addSetting(OAuthClientSettingName.TERMS_ACCEPTED, "true")
        client.addSetting(OAuthClientSettingName.SETUP_COMPLETED, "true")

        oAuthRegisteredClientRepository.save(client)

        command.phoneNumber.takeIf { it.isNotBlank() }?.let { phoneNumber ->
            runCatching {
                val locale = Locale.Builder().setRegion(command.country).build()
                val formattedPhone = PhoneNumber(phoneNumber, locale).toInternationalFormat()
                user.phoneNumber = formattedPhone
                userRepository.save(user)
                log.info { "Phone number saved to user profile: ${user.id}" }
            }.onFailure { e ->
                log.warn(e) { "Failed to save phone number to user profile, continuing with setup" }
            }
        }

        log.info { "Organization setup completed for merchant: ${client.clientId}, name: ${command.companyName}" }

        cacheEvictionService.evictMerchantCaches(merchantId.toString())

        return CompleteOrganizationSetupResult(
            success = true,
            message = messageService.getMessage("settings.success.setup_completed"),
            merchantId = merchantId.toString()
        )
    }
}
