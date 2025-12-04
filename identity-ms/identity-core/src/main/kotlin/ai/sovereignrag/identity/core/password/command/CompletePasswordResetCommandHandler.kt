package ai.sovereignrag.identity.core.password.command


import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.PROCESS_ID
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.REFERENCE
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.TOKEN
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.USERNAME
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.USER_ID
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.exception.UserNotFoundException
import ai.sovereignrag.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CompletePasswordResetCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Command.Handler<CompletePasswordResetCommand, CompletePasswordResetResult> {

    override fun handle(command: CompletePasswordResetCommand): CompletePasswordResetResult {
        log.info { "Completing password reset for reference: ${command.reference}" }

        val processId = UUID.fromString(command.reference)
        
        val process = processGateway.findPendingProcessByPublicId(
            id = processId
        ) ?: throw ClientException("Invalid or expired password reset reference")

        if (process.externalReference != command.token) {
            throw ClientException("Invalid password reset token")
        }

        val initialRequest = process.getInitialRequest()
        
        val userId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)?.let { UUID.fromString(it) }
            ?: throw ClientException("User ID not found in process")

        val user = userRepository.findById(userId).orElse(null)
            ?: throw UserNotFoundException("User not found with ID: $userId")

        user.password = passwordEncoder.encode(command.newPassword)
        user.resetFailedLoginAttempts()
        userRepository.save(user)

        val completeRequest = MakeProcessRequestPayload(
            userId = userId,
            processPublicId = processId,
            eventType = ProcessEvent.PROCESS_COMPLETED,
            requestType = ProcessRequestType.COMPLETE_PROCESS,
            channel = ProcessChannel.BUSINESS_WEB
        )
        
        processGateway.makeRequest(completeRequest)

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Password reset completed",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    PROCESS_ID.value to processId.toString(),
                    USERNAME.value to user.username,
                    REFERENCE.value to command.reference,
                    TOKEN.value to command.token,
                    USER_ID.value to userId.toString()
                )
            )
        )

        log.info { "Password reset completed successfully for user: ${user.username}" }

        return CompletePasswordResetResult(
            success = true,
            message = "Password reset completed successfully",
            userId = userId
        )
    }
}