package ai.sovereignrag.core.chat.strategy.impl

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import mu.KotlinLogging
import nl.compilot.ai.chat.service.MessageTranslationService
import nl.compilot.ai.chat.service.ResponseGenerationService
import nl.compilot.ai.chat.service.instruction.PromptInstructionService
import nl.compilot.ai.chat.strategy.ChatContext
import nl.compilot.ai.chat.strategy.ChatResponse
import nl.compilot.ai.chat.strategy.ChatResponseStrategy
import nl.compilot.ai.commons.UnansweredQueryLogger
import nl.compilot.ai.prompt.service.PersonaService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Strategy for handling queries using conversation history
 *
 * When no high-quality KB results are found and general knowledge is disabled,
 * but there is conversation history available, attempt to answer from the
 * context of the previous conversation.
 *
 * This allows the AI to answer follow-up questions that reference earlier
 * parts of the conversation even when the KB search doesn't return good results.
 */
@Component
@Order(80) // High priority - before falling back to "no results"
class ConversationHistoryStrategy(
    private val responseGenerationService: ResponseGenerationService,
    private val messageTranslationService: MessageTranslationService,
    private val personaService: PersonaService,
    private val instructionService: PromptInstructionService,
    private val unansweredQueryService: UnansweredQueryLogger
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // Can handle if:
        // - No high-quality KB results
        // - General knowledge is disabled
        // - We have conversation history to work with
        return !context.hasHighQualityResults &&
                !context.useGeneralKnowledge &&
                context.session.chatMemory.messages().isNotEmpty()
    }

    override fun priority(): Int = 80

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.info { "Attempting to answer from conversation history for query: '${context.message}'" }

        // Get language name for persona prompt
        val language = context.effectiveLanguage ?: "en"
        val languageName = messageTranslationService.getLanguageName(language)

        // Fetch persona configuration (cached) and assemble prompt
        val persona = personaService.getPersonaConfiguration(
            personaKey = context.session.persona,
            tenantId = null
        ) ?: throw IllegalArgumentException("Persona not found: ${context.session.persona}")

        val personaPrompt = personaService.assemblePersonaPrompt(
            persona = persona,
            runtimeParameters = mapOf(
                "language" to language,
                "languageName" to languageName,
                "includeSources" to "",
                "sourceFormat" to ""
            )
        )

        // Get no results message for fallback instruction
        val noResultsMessage = messageTranslationService.translateNoResultsMessage(context.effectiveLanguage)

        // Get conversation history instruction
        val conversationInstruction = instructionService.getConversationHistoryInstructions(
            noResultsMessage = noResultsMessage,
            tenantId = null
        )

        // Build system prompt with persona and language instruction
        val systemPrompt = personaPrompt + context.languageInstruction

        // Build user prompt with conversation history instruction
        val userPrompt = """
            ${context.message}

            $conversationInstruction
        """.trimIndent()

        // Add messages to chat memory
        context.session.chatMemory.add(SystemMessage(systemPrompt))
        context.session.chatMemory.add(UserMessage(userPrompt))

        // Generate response using conversation context
        val messages = context.session.chatMemory.messages()
        val aiResponse = responseGenerationService.generate(messages)

        val response = aiResponse?.text() ?: run {
            logger.error { "Failed to generate response with guardrails (conversation history)" }
            "I apologize, but I encountered an error. Please try again."
        }

        logger.info { "AI Response (from conversation history): $response" }

        // Add AI response to memory
        context.session.chatMemory.add(AiMessage(response))

        response.takeIf { it.contains(noResultsMessage, ignoreCase = true) }?.let {
            logger.warn { "No high-quality results and no conversation context for query: '${context.message}'" }
            unansweredQueryService.logUnansweredQuery(
                query = context.message,
                response = it,
                bestConfidence = context.searchResults.firstOrNull()?.confidence,
                usedGeneralKnowledge = false
            )
        }

        return ChatResponse(
            response = response,
            sources = emptyList(),
            needsConfidenceExtraction = false,
            shouldLogAsUnanswered = false, // Already logged above if needed
            usedGeneralKnowledge = false
        )
    }
}
