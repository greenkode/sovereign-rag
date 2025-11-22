package ai.sovereignrag.core.chat.command

import ai.sovereignrag.core.chat.dto.ChatStartResponse
import ai.sovereignrag.core.chat.service.ChatSessionManager
import ai.sovereignrag.core.chat.service.ConversationalAgentService
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class StartChatCommandHandler(
    private val chatSessionManager: ChatSessionManager,
    private val conversationalAgentService: ConversationalAgentService
) : Command.Handler<StartChatCommand, ChatStartResponse> {

    override fun handle(command: StartChatCommand): ChatStartResponse {
        logger.info { "Handling StartChatCommand with persona: ${command.persona}, language: ${command.language}" }

        // Create new chat session
        val session = chatSessionManager.createNewChatSession(
            persona = command.persona,
            language = command.language
        )

        logger.info { "Chat session started successfully: ${session.sessionId}" }

        // Frontend handles greeting display - backend just returns session ID
        return ChatStartResponse(
            sessionId = session.sessionId
        )
    }
}
