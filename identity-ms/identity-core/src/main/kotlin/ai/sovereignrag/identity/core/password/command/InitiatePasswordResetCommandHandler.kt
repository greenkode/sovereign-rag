package ai.sovereignrag.identity.core.password.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.commons.process.CreateNewProcessPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.integration.NotificationClient
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
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
class InitiatePasswordResetCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val processGateway: ProcessGateway,
    @Value("\${app.password-reset.base-url}")
    private val passwordResetBaseUrl: String,
    private val notificationClient: NotificationClient,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Command.Handler<InitiatePasswordResetCommand, InitiatePasswordResetResult> {

    private val log = KotlinLogging.logger {}

    override fun handle(command: InitiatePasswordResetCommand): InitiatePasswordResetResult {

        log.info { "Initiating password reset for email: ${command.email}" }

        val user = userRepository.findByUsername(command.email)
            ?: throw NotFoundException("User not found with email: ${command.email}")

        val processId = UUID.randomUUID()
        val token = RandomStringUtils.secure().nextAlphanumeric(100)

        if (processGateway.findRecentPendingProcessesByTypeAndForUserId(
                ProcessType.PASSWORD_RESET, user.id!!, Instant.now().minusSeconds(300)
            ).isNotEmpty()
        )
            return InitiatePasswordResetResult(
                reference = processId,
                success = true,
                message = "Password reset email sent successfully"
            )


        val process = processGateway.createProcess(
            CreateNewProcessPayload(
                userId = user.id!!,
                publicId = processId,
                type = ProcessType.PASSWORD_RESET,
                description = "Password reset for ${user.email}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.COMPLETE,
                channel = Channel.BUSINESS_WEB,
                externalReference = token,
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to user.id.toString(),
                    ProcessRequestDataName.AUTHENTICATION_REFERENCE to token
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to user.id.toString()
                )
            )
        )

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

        notificationClient.sendNotification(
            recipients = listOf(MessageRecipient(command.email, user.firstName)),
            templateName = TemplateName.PASSWORD_RESET,
            parameters = mapOf(
                "name" to (user.firstName ?: user.email),
                "reset_link" to "${passwordResetBaseUrl}?token=$token",
                "request_time" to formatter.format(Instant.now().atZone(ZoneId.systemDefault())),
                "expiry_time" to "${process.type.timeInSeconds / 60} minutes"
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString()
        )

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Password reset initiated - Email: ${command.email}",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    "processId" to processId.toString(),
                    "username" to user.username,
                    "email" to command.email,
                    "token" to token,
                    "userId" to user.id.toString()
                )
            )
        )

        log.info { "Password reset initiated successfully for email: ${command.email}" }

        return InitiatePasswordResetResult(
            reference = processId,
            success = true,
            message = "Password reset email sent successfully"
        )
    }
}