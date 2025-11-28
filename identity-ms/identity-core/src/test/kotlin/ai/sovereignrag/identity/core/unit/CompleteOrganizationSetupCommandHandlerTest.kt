package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.CompanyRole
import ai.sovereignrag.identity.core.entity.CompanySize
import ai.sovereignrag.identity.core.entity.IntendedPurpose
import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import ai.sovereignrag.identity.core.settings.command.CompleteOrganizationSetupCommand
import ai.sovereignrag.identity.core.settings.command.CompleteOrganizationSetupCommandHandler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteOrganizationSetupCommandHandlerTest {

    private val userService: UserService = mockk()
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository = mockk()
    private val cacheEvictionService: CacheEvictionService = mockk()
    private val messageService: MessageService = mockk()

    private lateinit var handler: CompleteOrganizationSetupCommandHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = CompleteOrganizationSetupCommandHandler(
            userService,
            oAuthRegisteredClientRepository,
            cacheEvictionService,
            messageService
        )
    }

    @Test
    fun `should complete organization setup successfully`() {
        val user = mockk<OAuthUser>()
        val client = mockk<OAuthRegisteredClient>(relaxed = true)
        val command = createCommand()

        every { userService.getCurrentUser() } returns user
        every { user.merchantId } returns merchantId
        every { oAuthRegisteredClientRepository.findById(merchantId.toString()) } returns Optional.of(client)
        every { client.getSetting(OAuthClientSettingName.SETUP_COMPLETED) } returns null
        every { client.clientId } returns "test-client-id"
        every { oAuthRegisteredClientRepository.save(any()) } returns client
        every { cacheEvictionService.evictMerchantCaches(any()) } just runs
        every { messageService.getMessage("settings.success.setup_completed") } returns "Setup completed"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Setup completed", result.message)
        assertEquals(merchantId.toString(), result.merchantId)

        verify { client.clientName = "Test Company" }
        verify { client.status = OrganizationStatus.ACTIVE }
        verify { client.addSetting(OAuthClientSettingName.INTENDED_PURPOSE, "CUSTOMER_SUPPORT") }
        verify { client.addSetting(OAuthClientSettingName.SETUP_COMPLETED, "true") }
        verify { oAuthRegisteredClientRepository.save(client) }
        verify { cacheEvictionService.evictMerchantCaches(merchantId.toString()) }
    }

    @Test
    fun `should throw exception when user has no merchant`() {
        val user = mockk<OAuthUser>()
        val command = createCommand()

        every { userService.getCurrentUser() } returns user
        every { user.merchantId } returns null
        every { messageService.getMessage("settings.error.no_merchant") } returns "No merchant associated"

        val exception = assertThrows<NotFoundException> {
            handler.handle(command)
        }

        assertEquals("No merchant associated", exception.message)
    }

    @Test
    fun `should throw exception when setup already completed`() {
        val user = mockk<OAuthUser>()
        val client = mockk<OAuthRegisteredClient>()
        val command = createCommand()

        every { userService.getCurrentUser() } returns user
        every { user.merchantId } returns merchantId
        every { oAuthRegisteredClientRepository.findById(merchantId.toString()) } returns Optional.of(client)
        every { client.getSetting(OAuthClientSettingName.SETUP_COMPLETED) } returns "true"
        every { messageService.getMessage("settings.error.setup_already_completed") } returns "Setup already completed"

        val exception = assertThrows<ClientException> {
            handler.handle(command)
        }

        assertEquals("Setup already completed", exception.message)
    }

    @Test
    fun `should throw exception when terms not accepted`() {
        val user = mockk<OAuthUser>()
        val client = mockk<OAuthRegisteredClient>()
        val command = createCommand(termsAccepted = false)

        every { userService.getCurrentUser() } returns user
        every { user.merchantId } returns merchantId
        every { oAuthRegisteredClientRepository.findById(merchantId.toString()) } returns Optional.of(client)
        every { client.getSetting(OAuthClientSettingName.SETUP_COMPLETED) } returns null
        every { messageService.getMessage("settings.error.terms_required") } returns "Terms must be accepted"

        val exception = assertThrows<ClientException> {
            handler.handle(command)
        }

        assertEquals("Terms must be accepted", exception.message)
    }

    private fun createCommand(
        companyName: String = "Test Company",
        intendedPurpose: IntendedPurpose = IntendedPurpose.CUSTOMER_SUPPORT,
        companySize: CompanySize = CompanySize.SIZE_11_50,
        roleInCompany: CompanyRole = CompanyRole.CTO,
        country: String = "US",
        phoneNumber: String = "+1234567890",
        termsAccepted: Boolean = true
    ) = CompleteOrganizationSetupCommand(
        companyName = companyName,
        intendedPurpose = intendedPurpose,
        companySize = companySize,
        roleInCompany = roleInCompany,
        country = country,
        phoneNumber = phoneNumber,
        termsAccepted = termsAccepted
    )
}
