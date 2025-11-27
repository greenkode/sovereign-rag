package ai.sovereignrag.identity.core.registration.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.commons.process.CreateNewProcessPayload
import ai.sovereignrag.identity.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ResendVerificationCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<ResendVerificationCommand, ResendVerificationResult> {

    override fun handle(command: ResendVerificationCommand): ResendVerificationResult {
        val normalizedEmail = command.email.trim().lowercase()
        log.info { "Resending verification email to: $normalizedEmail" }

        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw ClientException(messageService.getMessage("registration.error.user_not_found"))

        if (user.emailVerified) {
            throw ClientException(messageService.getMessage("registration.error.email_already_verified"))
        }

        processGateway.findPendingProcessByTypeAndExternalReference(
            type = ProcessType.EMAIL_VERIFICATION,
            externalReference = normalizedEmail
        )?.let { existingProcess ->
            val cancelRequest = MakeProcessRequestPayload(
                userId = user.id!!,
                processPublicId = existingProcess.publicId,
                eventType = ProcessEvent.PROCESS_FAILED,
                requestType = ProcessRequestType.FAIL_PROCESS,
                channel = Channel.BUSINESS_WEB
            )
            processGateway.makeRequest(cancelRequest)
        }

        val verificationToken = UUID.randomUUID().toString()
        val processPayload = CreateNewProcessPayload(
            userId = user.id!!,
            publicId = UUID.randomUUID(),
            type = ProcessType.EMAIL_VERIFICATION,
            description = "Email verification resend for ${user.email}",
            initialState = ProcessState.PENDING,
            requestState = ProcessState.PENDING,
            channel = Channel.BUSINESS_WEB,
            data = mapOf(
                ProcessRequestDataName.USER_EMAIL to normalizedEmail,
                ProcessRequestDataName.USER_IDENTIFIER to user.id.toString(),
                ProcessRequestDataName.VERIFICATION_TOKEN to verificationToken
            ),
            stakeholders = mapOf(
                ProcessStakeholderType.FOR_USER to user.id.toString()
            ),
            externalReference = verificationToken
        )

        processGateway.createProcess(processPayload)
        log.info { "Verification email resent for user: ${user.id}" }

        return ResendVerificationResult(
            success = true,
            message = messageService.getMessage("registration.success.verification_resent")
        )
    }
}
