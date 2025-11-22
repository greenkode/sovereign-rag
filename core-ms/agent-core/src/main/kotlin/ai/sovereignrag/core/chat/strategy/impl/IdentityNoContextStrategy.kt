package ai.sovereignrag.core.chat.strategy.impl

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import mu.KotlinLogging
import ai.sovereignrag.chat.service.MessageTranslationService
import ai.sovereignrag.chat.service.ResponseGenerationService
import ai.sovereignrag.chat.service.instruction.PromptInstructionService
import ai.sovereignrag.chat.strategy.ChatContext
import ai.sovereignrag.chat.strategy.ChatResponse
import ai.sovereignrag.chat.strategy.ChatResponseStrategy
import ai.sovereignrag.prompt.service.PersonaService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Strategy for handling identity questions without KB context
 *
 * When the user asks "who are you?", "what can you do?", etc. and
 * there's no relevant content in the knowledge base, provide a simple
 * self-introduction based on the persona prompt.
 *
 * This strategy requires confidence extraction as the LLM is asked to
 * provide a confidence score with its response.
 */
@Component
@Order(90) // Very high priority - identity questions are important
class IdentityNoContextStrategy(
    private val responseGenerationService: ResponseGenerationService,
    private val messageTranslationService: MessageTranslationService,
    private val personaService: PersonaService,
    private val instructionService: PromptInstructionService
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // NEVER handle - identity queries should NOT bypass KB results
        // Identity is handled by customizing prompts in GeneralKnowledgeNoContextStrategy
        return false
    }

    override fun priority(): Int = 0 // Disabled

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.info { "Handling identity question without context: '${context.message}'" }

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

        // Get identity instructions (no context version)
        val identityInstructions = instructionService.getIdentityNoContextInstructions(
            tenantId = null
        )

        // Get language prefix
        val languagePrefix = instructionService.getLanguagePrefix(
            language = context.effectiveLanguage,
            getLanguageName = messageTranslationService::getLanguageName,
            tenantId = null
        )

        // Build system prompt with persona and language instruction
        val systemPrompt = personaPrompt + context.languageInstruction

        // Build user prompt with identity instructions
        val userPrompt = """
            ${languagePrefix}Question: ${context.message}

            $identityInstructions
        """.trimIndent()

        // Add messages to chat memory
        context.session.chatMemory.add(SystemMessage(systemPrompt))
        context.session.chatMemory.add(UserMessage(userPrompt))

        // Generate response
        val messages = context.session.chatMemory.messages()
        val aiResponse = responseGenerationService.generate(messages)

        val response = aiResponse?.text() ?: run {
            logger.error { "Failed to generate response with guardrails (identity no context)" }
            "I apologize, but I encountered an error. Please try again."
        }

        logger.info { "AI Response (identity no context): $response" }

        // Add AI response to memory
        context.session.chatMemory.add(AiMessage(response))

        return ChatResponse(
            response = response,
            sources = emptyList(),
            needsConfidenceExtraction = true, // Identity responses include [CONFIDENCE: XX%]
            shouldLogAsUnanswered = false, // Identity questions shouldn't be logged as unanswered
            usedGeneralKnowledge = true
        )
    }
}
