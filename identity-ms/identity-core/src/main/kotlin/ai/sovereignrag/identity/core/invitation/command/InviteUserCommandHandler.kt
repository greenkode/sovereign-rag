package ai.sovereignrag.identity.core.invitation.command

import ai.sovereignrag.identity.commons.RoleEnum
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.process.CreateNewProcessPayload
import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.TrustLevel
import ai.sovereignrag.identity.core.entity.UserType
import ai.sovereignrag.identity.core.integration.NotificationClient
import ai.sovereignrag.identity.core.invitation.dto.InviteUserCommand
import ai.sovereignrag.identity.core.invitation.dto.InviteUserResult
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
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

@Component
@Transactional
class InviteUserCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val clientRepository: OAuthRegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val processGateway: ProcessGateway,
    private val notificationClient: NotificationClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${app.merchant.invitation.base-url}")
    private val invitationBaseUrl: String
) : Command.Handler<InviteUserCommand, InviteUserResult> {

    private val log = KotlinLogging.logger {}

    override fun handle(command: InviteUserCommand): InviteUserResult {

        log.info { "Processing user invitation for email: ${command.userEmail}" }

        val invitingUser = userRepository.findById(UUID.fromString(command.invitedByUserId))
            .orElseThrow { RecordNotFoundException("Inviting user not found") }

        val merchant = clientRepository.findById(invitingUser.merchantId.toString())
            .orElseThrow { RecordNotFoundException("Merchant not found") }

        userRepository.findByUsername(command.userEmail)?.let { user -> throw InvalidRequestException("User already exists") }

        if(command.role == RoleEnum.ROLE_MERCHANT_SUPER_ADMIN) {
            throw InvalidRequestException("Only business owner can be super admins")
        }

        val newUser = userRepository.save(
            OAuthUser(
                username = command.userEmail,
                password = passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(12)),
                email = command.userEmail,
                enabled = true,
                authorities = mutableSetOf(RoleEnum.ROLE_MERCHANT_USER.value, command.role.value)
            ).apply {
                userType = UserType.BUSINESS
                trustLevel = TrustLevel.TIER_THREE
                emailVerified = false
                merchantId = invitingUser.merchantId
            }
        )

        log.info { "Creating user invitation process for ${newUser.email}" }

        val processId = UUID.randomUUID()
        val token = RandomStringUtils.secure().nextAlphanumeric(100)
        val authReference = UUID.randomUUID().toString()

        val process = processGateway.createProcess(
            CreateNewProcessPayload(
                userId = newUser.id!!,
                publicId = processId,
                type = ProcessType.MERCHANT_USER_INVITATION,
                description = "User invitation for ${newUser.email} to join ${merchant.clientName}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.COMPLETE,
                channel = ProcessChannel.SYSTEM,
                externalReference = token,
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to newUser.id.toString(),
                    ProcessRequestDataName.MERCHANT_ID to merchant.id,
                    ProcessRequestDataName.AUTHENTICATION_REFERENCE to authReference
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to newUser.id.toString()
                )
            )
        )

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        notificationClient.sendNotification(
            recipients = listOf(MessageRecipient(address = newUser.email)),
            templateName = TemplateName.MERCHANT_USER_INVITATION,
            parameters = mapOf(
                "merchant_name" to merchant.clientName,
                "invited_by" to "${invitingUser.firstName ?: ""} ${invitingUser.lastName ?: ""}".trim()
                    .ifEmpty { invitingUser.email },
                "invitation_url" to "$invitationBaseUrl?token=${process.externalReference}",
                "expiration_date" to formatter.format(
                    Instant.now().plusSeconds(process.type.timeInSeconds).atZone(ZoneId.systemDefault())
                )
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString()
        )

        log.info { "User invitation sent successfully to ${newUser.email}" }

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = invitingUser.id.toString(),
                actorName = "${invitingUser.firstName ?: ""} ${invitingUser.lastName ?: ""}".trim()
                    .ifEmpty { invitingUser.email },
                merchantId = newUser.merchantId.toString(),
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User ${command.userEmail} invited to merchant",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
            )
        )

        log.info { "Successfully invited user ${command.userEmail} to merchant ${merchant.clientName}" }

        return InviteUserResult(
            success = true,
            message = "User invitation sent successfully",
            invitationId = processId.toString()
        )
    }
}