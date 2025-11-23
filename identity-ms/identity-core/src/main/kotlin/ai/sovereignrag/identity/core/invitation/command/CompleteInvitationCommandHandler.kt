package ai.sovereignrag.identity.core.invitation.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.exception.UserNotFoundException
import ai.sovereignrag.identity.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CompleteInvitationCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Command.Handler<CompleteInvitationCommand, CompleteInvitationResult> {

    override fun handle(command: CompleteInvitationCommand): CompleteInvitationResult {
        log.info { "Completing invitation for token: ${command.token}" }

        val process = processGateway.findPendingProcessByTypeAndExternalReference(
            type = ProcessType.MERCHANT_USER_INVITATION,
            externalReference = command.token
        ) ?: throw ClientException("Invalid or expired invitation token")

        val initialRequest = process.getInitialRequest()
        val authReference = initialRequest.getDataValueOrNull(ProcessRequestDataName.AUTHENTICATION_REFERENCE)

        if (authReference != command.reference) {
            throw ClientException("Invalid invitation reference")
        }

        val userId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)
            ?.let { UUID.fromString(it) }
            ?: throw ClientException("User ID not found in invitation")

        val user = userRepository.findByAkuId(userId)
            ?: throw UserNotFoundException("User not found with ID: $userId")

        val nameParts = command.fullName.trim().split("\\s+".toRegex(), 2)
        user.firstName = nameParts[0]
        user.lastName = if (nameParts.size > 1) nameParts[1] else ""
        user.emailVerified = true
        user.invitationStatus = true
        user.password = passwordEncoder.encode(command.password)
        user.phoneNumber = command.phoneNumber.value

        userRepository.save(user)

        val completeRequest = MakeProcessRequestPayload(
            userId = userId,
            processPublicId = process.publicId,
            eventType = ProcessEvent.PROCESS_COMPLETED,
            requestType = ProcessRequestType.COMPLETE_PROCESS,
            channel = Channel.BUSINESS_WEB
        )
        
        processGateway.makeRequest(completeRequest)

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User invitation completed successfully",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
            )
        )

        log.info { "User ${user.username} profile updated successfully" }

        return CompleteInvitationResult(
            success = true,
            message = "Invitation completed successfully",
            userId = userId
        )
    }
}