package ai.sovereignrag.identity.core.password.command

import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ValidatePasswordResetCommandHandler(
    private val processGateway: ProcessGateway,
) : Command.Handler<ValidatePasswordResetCommand, ValidatePasswordResetResult> {

    override fun handle(command: ValidatePasswordResetCommand): ValidatePasswordResetResult {

        log.info { "Validating password reset for token: ${command.token}" }

        val process = processGateway.findPendingProcessByExternalReference(
            externalReference = command.token
        ) ?: throw NotFoundException("Invalid or expired password reset token")

        val initialRequest = process.getInitialRequest()

        initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)
            ?: throw NotFoundException("User identifier not found in process")

        val userId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)?.let { UUID.fromString(it) }
            ?: throw NotFoundException("User ID not found in process")

        return ValidatePasswordResetResult(
            success = true,
            message = "Password reset token validated successfully",
            reference = process.publicId,
            userId = userId
        )
    }
}