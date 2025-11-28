package ai.sovereignrag.identity.core.settings.query

import ai.sovereignrag.identity.core.entity.CompanyRole
import ai.sovereignrag.identity.core.entity.CompanySize
import ai.sovereignrag.identity.core.entity.IntendedPurpose
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
        val intendedPurpose = client.getSetting(OAuthClientSettingName.INTENDED_PURPOSE)
            ?.let { runCatching { IntendedPurpose.valueOf(it) }.getOrNull() }
        val companySize = client.getSetting(OAuthClientSettingName.COMPANY_SIZE)
            ?.let { runCatching { CompanySize.valueOf(it) }.getOrNull() }
        val roleInCompany = client.getSetting(OAuthClientSettingName.ROLE_IN_COMPANY)
            ?.let { runCatching { CompanyRole.valueOf(it) }.getOrNull() }
        val country = client.getSetting(OAuthClientSettingName.COUNTRY)
        val phoneNumber = client.getSetting(OAuthClientSettingName.PHONE_NUMBER)
        val website = client.getSetting(OAuthClientSettingName.WEBSITE)
        val email = client.getSetting(OAuthClientSettingName.EMAIL)

        return GetOrganizationDetailsResult(
            id = client.id,
            name = client.clientName,
            plan = client.plan,
            status = client.status,
            environmentMode = client.environmentMode,
            setupCompleted = setupCompleted,
            intendedPurpose = intendedPurpose,
            companySize = companySize,
            roleInCompany = roleInCompany,
            country = country,
            phoneNumber = phoneNumber,
            website = website,
            email = email
        )
    }
}
