package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import ai.sovereignrag.chat.service.ChatSessionManager
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CloseChatCommandHandler(
    private val chatSessionManager: ChatSessionManager
) : Command.Handler<CloseChatCommand, Map<String, Any>> {

    override fun handle(command: CloseChatCommand): Map<String, Any> {
        logger.info { "Handling CloseChatCommand for session: ${command.sessionId}" }

        // Terminate the chat session
        chatSessionManager.terminateChatSession(command.sessionId)

        logger.info { "Chat session closed successfully: ${command.sessionId}" }

        return mapOf(
            "success" to true,
            "message" to "Chat session closed",
            "sessionId" to command.sessionId
        )
    }
}
