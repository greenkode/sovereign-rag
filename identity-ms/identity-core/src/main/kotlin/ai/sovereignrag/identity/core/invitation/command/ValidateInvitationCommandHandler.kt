package ai.sovereignrag.identity.core.invitation.command

import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class ValidateInvitationCommandHandler(
    private val processGateway: ProcessGateway
) : Command.Handler<ValidateInvitationCommand, ValidateInvitationResult> {

    override fun handle(command: ValidateInvitationCommand): ValidateInvitationResult {
        log.info { "Validating invitation token: ${command.token}" }

        val process = processGateway.findPendingProcessByTypeAndExternalReference(
            type = ProcessType.MERCHANT_USER_INVITATION,
            externalReference = command.token
        ) ?: throw NotFoundException("Invalid or expired invitation token")

        val initialRequest = process.getInitialRequest()
        
        val authReference = initialRequest.getDataValueOrNull(ProcessRequestDataName.AUTHENTICATION_REFERENCE)
            ?: UUID.randomUUID().toString()

        log.info { "Invitation token validated successfully: ${command.token}" }

        return ValidateInvitationResult(
            success = true,
            token = command.token,
            authReference = authReference
        )
    }
}