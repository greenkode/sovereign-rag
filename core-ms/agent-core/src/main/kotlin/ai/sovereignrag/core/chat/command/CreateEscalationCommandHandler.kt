package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import ai.sovereignrag.commons.EscalationLogger
import ai.sovereignrag.core.chat.dto.EscalationResponse
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class CreateEscalationCommandHandler(
    private val escalationLogger: EscalationLogger
) : Command.Handler<CreateEscalationCommand, EscalationResponse> {

    override fun handle(command: CreateEscalationCommand): EscalationResponse {
        logger.info { "Handling CreateEscalationCommand for session: ${command.sessionId}" }

        // Log escalation using the interface
        val escalationId = escalationLogger.logEscalation(
            sessionId = UUID.fromString(command.sessionId),
            reason = command.reason,
            userEmail = command.userEmail,
            userName = command.userName ?: "Unknown",
            userPhone = command.userPhone,
            userMessage = command.userMessage,
            language = null,
            persona = "customer_service",
            emailSent = false // Emails are sent separately via EmailTool
        )

        logger.info { "Escalation created successfully: $escalationId" }

        return EscalationResponse(
            success = true,
            message = "Your request has been submitted to our support team. You will receive a confirmation email shortly.",
            escalationId = escalationId.toString()
        )
    }
}
