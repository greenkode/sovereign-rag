package ai.sovereignrag.core.chat.api

import an.awesome.pipelinr.Pipeline
import mu.KotlinLogging
import ai.sovereignrag.chat.command.CloseChatCommand
import ai.sovereignrag.chat.command.CreateEscalationCommand
import ai.sovereignrag.chat.command.SendMessageCommand
import ai.sovereignrag.chat.command.StartChatCommand
import ai.sovereignrag.chat.dto.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/agent/chat")
class ChatController(
    private val pipeline: Pipeline
) {

    @PostMapping("/start")
    fun startChat(@RequestBody request: ChatStartRequest): ChatStartResponse {
        logger.info { "Starting chat session with persona: ${request.persona}, language: ${request.language}" }

        val command = StartChatCommand(
            persona = request.persona,
            language = request.language
        )

        return pipeline.send(command)
    }

    @PostMapping("/{sessionId}/message")
    fun sendMessage(
        @PathVariable sessionId: String,
        @RequestBody request: ChatMessageRequest
    ): ChatMessageResponse {
        logger.info { "Chat message for session $sessionId: ${request.message}" }

        val command = SendMessageCommand(
            sessionId = sessionId,
            message = request.message,
            useGeneralKnowledge = request.useGeneralKnowledge,
            showGkDisclaimer = request.showGkDisclaimer,
            gkDisclaimerText = request.gkDisclaimerText,
            showSources = request.showSources
        )

        return try {
            pipeline.send(command)
        } catch (e: IllegalArgumentException) {
            logger.error { "Session not found: $sessionId" }
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found: $sessionId", e)
        }
    }

    @PostMapping("/{sessionId}/close")
    fun closeChat(@PathVariable sessionId: String): Map<String, Any> {
        logger.info { "Closing chat session: $sessionId" }

        val command = CloseChatCommand(sessionId = sessionId)

        return pipeline.send(command)
    }

    @PostMapping("/{sessionId}/escalate")
    fun escalateChat(
        @PathVariable sessionId: String,
        @RequestBody request: EscalationRequest
    ): EscalationResponse {
        logger.info { "Escalating chat session $sessionId for user: ${request.userEmail}" }

        val command = CreateEscalationCommand(
            sessionId = sessionId,
            userEmail = request.userEmail,
            userName = request.userName,
            userPhone = request.userPhone,
            userMessage = request.userMessage,
            reason = request.reason
        )

        return try {
            pipeline.send(command)
        } catch (e: IllegalArgumentException) {
            logger.error { "Session not found: $sessionId" }
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found: $sessionId", e)
        }
    }
}
