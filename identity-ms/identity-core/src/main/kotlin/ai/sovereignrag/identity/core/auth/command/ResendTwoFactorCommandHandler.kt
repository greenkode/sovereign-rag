package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.TwoFactorSessionInvalidException
import ai.sovereignrag.identity.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.TokenGenerationUtility
import ai.sovereignrag.identity.core.auth.service.TwoFactorEmailService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ResendTwoFactorCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val twoFactorEmailService: TwoFactorEmailService,
    private val tokenGenerationUtility: TokenGenerationUtility,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${identity.2fa.token-length:6}") private val tokenLength: Int = 6
) : Command.Handler<ResendTwoFactorCommand, ResendTwoFactorResult> {

    override fun handle(command: ResendTwoFactorCommand): ResendTwoFactorResult {
        log.info { "2FA resend attempt for session: ${command.sessionId}" }

        val process =
            processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, command.sessionId)
                ?: throw TwoFactorSessionInvalidException("Invalid session")

        if (process.state != ProcessState.PENDING) {
            log.warn { "Cannot resend 2FA code for process in state: ${process.state}" }
            throw TwoFactorSessionInvalidException("Session is no longer valid")
        }

        val userId = UUID.fromString(process.getInitialRequest().getStakeholderValue(ProcessStakeholderType.FOR_USER))

        val user = userRepository.findById(userId).orElseThrow {
            IllegalStateException("User not found")
        }

        val newCode = tokenGenerationUtility.generateNumericToken(tokenLength)

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                userId,
                process.publicId,
                ProcessEvent.AUTH_TOKEN_RESEND,
                ProcessRequestType.RESEND_AUTHENTICATION,
                Channel.API,
                data = mapOf(ProcessRequestDataName.AUTHENTICATION_REFERENCE to newCode)
            )
        )

        twoFactorEmailService.sendTwoFactorCode(user, newCode, command.ipAddress)

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Two-factor authentication code resent",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf<String, String>(
                    "processId" to process.publicId.toString(),
                    "username" to user.username,
                    "sessionId" to command.sessionId,
                    "ipAddress" to (command.ipAddress ?: "unknown"),
                    "newCode" to newCode,
                    "userId" to user.id.toString()
                )
            )
        )

        log.info { "2FA code resent to user ${user.username}" }

        return ResendTwoFactorResult(
            sessionId = command.sessionId,
            message = "New verification code sent!"
        )
    }
}