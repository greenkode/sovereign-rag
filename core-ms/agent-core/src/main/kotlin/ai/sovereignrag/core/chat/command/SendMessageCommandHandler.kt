package ai.sovereignrag.core.chat.command

import ai.sovereignrag.core.chat.dto.ChatMessageResponse
import ai.sovereignrag.core.chat.service.ChatSessionManager
import ai.sovereignrag.core.chat.service.ConversationalAgentService
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SendMessageCommandHandler(
    private val chatSessionManager: ChatSessionManager,
    private val conversationalAgentService: ConversationalAgentService
) : Command.Handler<SendMessageCommand, ChatMessageResponse> {

    override fun handle(command: SendMessageCommand): ChatMessageResponse {
        logger.info { "Handling SendMessageCommand for session: ${command.sessionId} (showSources: ${command.showSources})" }

        // Retrieve active chat session
        val session = chatSessionManager.findActiveChatSession(command.sessionId)
            ?: throw IllegalArgumentException("Chat session not found: ${command.sessionId}")

        // Process message through conversational agent
        val result = conversationalAgentService.processChatInteraction(
            session = session,
            message = command.message,
            useGeneralKnowledge = command.useGeneralKnowledge,
            showGkDisclaimer = command.showGkDisclaimer,
            gkDisclaimerText = command.gkDisclaimerText,
            showSources = command.showSources
        )

        // Save the updated session back to cache (preserves chat history)
        chatSessionManager.updateChatSession(session)

        logger.info { "Message processed successfully for session: ${command.sessionId} (confidence: ${result.confidenceScore}%)" }

        // Strip sources from response text if showSources is disabled
        val finalResponse = if (!command.showSources) {
            stripSourcesFromResponse(result.response)
        } else {
            result.response
        }

        return ChatMessageResponse(
            response = finalResponse,
            sources = if (command.showSources) result.sources else emptyList(),
            suggestsClose = result.userSeemsFinished,
            confidenceScore = result.confidenceScore,
            showConfidence = result.showConfidence
        )
    }

    /**
     * Remove source citations from Markdown response
     * Matches patterns like: [Source 1](URL) | [Source 2](URL)
     */
    private fun stripSourcesFromResponse(response: String): String {
        // Remove Markdown links that look like source citations
        // Pattern: [Source N](URL) or [any text](URL) at end of lines/paragraphs
        return response
            .replace(Regex("\\[Source\\s+\\d+\\]\\([^)]+\\)"), "")
            .replace(Regex("\\s*\\|\\s*\\[Source\\s+\\d+\\]\\([^)]+\\)"), "")
            .replace(Regex("\\[Learn more\\]\\([^)]+\\)"), "")
            .replace(Regex("\\[Check this out\\]\\([^)]+\\)"), "")
            .replace(Regex("\\[Technical Documentation\\]\\([^)]+\\)"), "")
            .replace(Regex("\\[Documentation\\]\\([^)]+\\)"), "")
            .replace(Regex("\\[Reference\\]\\([^)]+\\)"), "")
            .trim()
            .replace(Regex("\\n{3,}"), "\n\n") // Clean up excessive newlines
    }
}
