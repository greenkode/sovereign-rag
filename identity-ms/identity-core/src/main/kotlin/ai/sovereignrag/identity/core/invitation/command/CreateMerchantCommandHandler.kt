package ai.sovereignrag.identity.core.invitation.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.notification.MessagePayload
import ai.sovereignrag.identity.commons.notification.MessageRecipient
import ai.sovereignrag.identity.commons.process.CreateNewProcessPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.TrustLevel
import ai.sovereignrag.identity.core.entity.UserType
import ai.sovereignrag.identity.core.invitation.dto.CreateMerchantResult
import ai.sovereignrag.identity.core.integration.CoreMerchantClient
import ai.sovereignrag.identity.core.invitation.dto.CreateMerchantCommand
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CreateMerchantCommandHandler(
    private val repository: OAuthRegisteredClientRepository,
    private val userRepository: OAuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    private val processGateway: ProcessGateway,
    private val coreMerchantClient: CoreMerchantClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${app.merchant.invitation.base-url}")
    private val invitationBaseUrl: String
) : Command.Handler<CreateMerchantCommand, CreateMerchantResult> {

    override fun handle(command: CreateMerchantCommand): CreateMerchantResult {

        val convertedClientId = command.clientName.lowercase().replace(" ", "-")

        log.info { "Creating new merchant with clientId: $convertedClientId" }

        val newMerchantId = UUID.randomUUID()

        val adminUser = userRepository.findByUsername(command.adminEmail)
            ?: run {

                log.info { "Creating new admin user for email: ${command.adminEmail}" }

                userRepository.save(
                    OAuthUser(
                        username = command.adminEmail,
                        password = passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(12)),
                        email = command.adminEmail,
                        enabled = true,
                        authorities = mutableSetOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_USER"),
                    ).apply {
                        userType = UserType.BUSINESS
                        trustLevel = TrustLevel.TIER_THREE
                        emailVerified = false
                        akuId = UUID.randomUUID()
                        merchantId = newMerchantId
                    }
                )
            }

        val existingClient = repository.findByClientId(convertedClientId)
            ?: run {

                val sandboxSecret = RandomStringUtils.secure().nextAlphanumeric(30)
                val productionSecret = RandomStringUtils.secure().nextAlphanumeric(30)

                val client = OAuthRegisteredClient().apply {
                    id = newMerchantId.toString()
                    clientId = UUID.randomUUID().toString()
                    clientName = command.clientName
                    clientIdIssuedAt = Instant.now()
                    clientSecret = passwordEncoder.encode(sandboxSecret)
                    sandboxClientSecret = passwordEncoder.encode(sandboxSecret)
                    productionClientSecret = passwordEncoder.encode(productionSecret)
                    clientAuthenticationMethods = "client_secret_basic,client_secret_post"
                    authorizationGrantTypes = "client_credentials,refresh_token"
                    scopes = "openid,profile,email,read,write,merchant"
                    clientSettings = objectMapper.writeValueAsString(
                        mapOf(
                            "requireAuthorizationConsent" to "false",
                            "requireProofKey" to "false",
                            "email" to command.adminEmail,
                        )
                    )
                    tokenSettings = objectMapper.writeValueAsString(
                        mapOf(
                            "accessTokenTimeToLive" to "PT30M",
                            "refreshTokenTimeToLive" to "PT12H",
                            "reuseRefreshTokens" to "false"
                        )
                    )
                    failedAuthAttempts = 0
                }

                repository.save(client)
            }

        log.info { "Creating merchant invitation process for ${adminUser.email}" }

        val processId = UUID.randomUUID()
        val token = RandomStringUtils.secure().nextAlphanumeric(100)
        val authReference = UUID.randomUUID().toString()

        val process = processGateway.createProcess(
            CreateNewProcessPayload(
                userId = adminUser.akuId!!,
                publicId = processId,
                type = ProcessType.MERCHANT_USER_INVITATION,
                description = "Merchant invitation for ${adminUser.email} to join ${existingClient.clientName}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.COMPLETE,
                channel = Channel.SYSTEM,
                externalReference = token,
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to adminUser.akuId.toString(),
                    ProcessRequestDataName.MERCHANT_ID to existingClient.id,
                    ProcessRequestDataName.AUTHENTICATION_REFERENCE to authReference
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to adminUser.akuId.toString()
                )
            )
        )

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        coreMerchantClient.sendMessage(
            MessagePayload(
                listOf(MessageRecipient(address = adminUser.email)),
                "MERCHANT_USER_INVITATION", "EMAIL", "HIGH",
                mapOf(
                    "merchant_name" to existingClient.clientName,
                    "invitation_url" to "$invitationBaseUrl?token=${process.externalReference}",
                    "expiration_date" to formatter.format(
                        Instant.now().plusSeconds(process.type.timeInSeconds).atZone(ZoneId.systemDefault())
                    )
                ), Locale.ENGLISH, UUID.randomUUID().toString(), "INDIVIDUAL"
            )
        )

        log.info { "Merchant invitation sent successfully to ${adminUser.email}" }

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = adminUser.id.toString(),
                actorName = "${adminUser.firstName ?: ""} ${adminUser.lastName ?: ""}".trim().ifEmpty { adminUser.email },
                merchantId = newMerchantId.toString(),
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Merchant created with invitation sent",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
            )
        )

        log.info { "Successfully created merchant with clientId: ${existingClient.clientId}" }

        return CreateMerchantResult(
            success = true,
            message = "Merchant created successfully",
            existingClient.id
        )
    }

}