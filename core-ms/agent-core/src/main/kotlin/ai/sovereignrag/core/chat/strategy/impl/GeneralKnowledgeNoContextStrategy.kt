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
import nl.compilot.ai.prompt.service.PersonaService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Strategy for answering using general knowledge without KB context
 *
 * When general knowledge is enabled and there are no high-quality KB results,
 * answer the question using the LLM's general knowledge. This is for normal
 * questions (not identity queries) where the KB doesn't have relevant information.
 *
 * A disclaimer is shown if configured to indicate the response is from general
 * knowledge rather than the knowledge base.
 *
 * This strategy requires confidence extraction as the LLM is asked to
 * provide a confidence score with its response.
 */
@Component
@Order(70) // Medium-high priority - general knowledge fallback
class GeneralKnowledgeNoContextStrategy(
    private val responseGenerationService: ResponseGenerationService,
    private val messageTranslationService: MessageTranslationService,
    private val personaService: PersonaService,
    private val instructionService: PromptInstructionService
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // Can handle if:
        // - General knowledge is enabled
        // - No high-quality KB results (no context to work with)
        // Note: Also handles identity queries by customizing the prompt
        return context.useGeneralKnowledge &&
                !context.hasHighQualityResults
    }

    override fun priority(): Int = 70

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.info { "Answering using general knowledge (no context): '${context.message}'" }

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

        // Get language prefix
        val languagePrefix = instructionService.getLanguagePrefix(
            language = context.effectiveLanguage,
            getLanguageName = messageTranslationService::getLanguageName,
            tenantId = null
        )

        // Build system prompt with persona and language instruction
        val systemPrompt = personaPrompt + context.languageInstruction

        // Build user prompt - different for identity vs non-identity queries
        val userPrompt = if (context.isIdentityQuery) {
            // Special handling for identity questions
            val identityInstructions = instructionService.getIdentityNoContextInstructions(
                tenantId = null
            )
            """
                ${languagePrefix}Question: ${context.message}

                $identityInstructions
            """.trimIndent()
        } else {
            // Normal general knowledge without context
            val disclaimerInstruction = context.gkDisclaimerText
                .takeIf { context.showGkDisclaimer }
                ?.let { "\n\nIMPORTANT: Add this disclaimer at the end of your response:\n$it" }
                ?: ""

            val generalKnowledgeInstructions = instructionService.getGeneralKnowledgeNoContextInstructions(
                disclaimerInstruction = disclaimerInstruction,
                tenantId = null
            )

            """
                ${languagePrefix}Question: ${context.message}

                $generalKnowledgeInstructions
            """.trimIndent()
        }

        // Add messages to chat memory
        context.session.chatMemory.add(SystemMessage(systemPrompt))
        context.session.chatMemory.add(UserMessage(userPrompt))

        // Generate response
        val messages = context.session.chatMemory.messages()
        val aiResponse = responseGenerationService.generate(messages)

        val response = aiResponse?.text() ?: run {
            logger.error { "Failed to generate response with guardrails (general knowledge no context)" }
            "I apologize, but I encountered an error. Please try again."
        }

        logger.info { "AI Response (general knowledge no context): $response" }

        // Add AI response to memory
        context.session.chatMemory.add(AiMessage(response))

        return ChatResponse(
            response = response,
            sources = emptyList(),
            needsConfidenceExtraction = true, // General knowledge responses include [CONFIDENCE: XX%]
            shouldLogAsUnanswered = false, // General knowledge responses shouldn't be logged as unanswered
            usedGeneralKnowledge = true
        )
    }
}
