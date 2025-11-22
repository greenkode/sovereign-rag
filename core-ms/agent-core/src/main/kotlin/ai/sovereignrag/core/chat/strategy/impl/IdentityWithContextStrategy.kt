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
import nl.compilot.ai.domain.SearchResult
import nl.compilot.ai.prompt.service.PersonaService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Strategy for handling identity questions with KB context
 *
 * When the user asks "who are you?", "what can you do?", etc. and
 * there IS relevant content in the knowledge base (e.g., "About" page),
 * provide a self-introduction that includes relevant organizational info
 * from the KB while staying focused on identity.
 *
 * This strategy requires confidence extraction as the LLM is asked to
 * provide a confidence score with its response.
 */
@Component
@Order(95) // Highest priority - identity questions with context are most specific
class IdentityWithContextStrategy(
    private val responseGenerationService: ResponseGenerationService,
    private val messageTranslationService: MessageTranslationService,
    private val personaService: PersonaService,
    private val instructionService: PromptInstructionService
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // NEVER handle - identity queries should NOT bypass KB results
        // Identity is handled by customizing prompts in other strategies
        return false
    }

    override fun priority(): Int = 0 // Disabled

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.info { "Handling identity question with context: '${context.message}'" }

        // Get language name for persona prompt
        val language = context.effectiveLanguage ?: "en"
        val languageName = messageTranslationService.getLanguageName(language)

        // Fetch persona configuration (cached) and assemble prompt
        val persona = personaService.getPersonaConfiguration(
            personaKey = context.session.persona,
            tenantId = null // TODO: Get from TenantContext if needed
        ) ?: throw IllegalArgumentException("Persona not found: ${context.session.persona}")

        val personaPrompt = personaService.assemblePersonaPrompt(
            persona = persona,
            runtimeParameters = mapOf(
                "language" to language,
                "languageName" to languageName,
                "includeSources" to if (context.showSources) "- Include source links from the context at the end of your answer." else "",
                "sourceFormat" to if (context.showSources) "[Source 1](URL1) | [Source 2](URL2)" else ""
            )
        )

        // Format context from search results
        val contextText = formatContext(context.searchResults, context.showSources)

        // Extract sources for potential citation
        val sources = context.takeIf { it.showSources }
            ?.searchResults
            ?.mapNotNull { it.source }
            ?.distinct()
            ?: emptyList()

        // Get identity instructions (with context version)
        val identityInstructions = instructionService.getIdentityWithContextInstructions(
            showSources = context.showSources,
            sources = sources,
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

        // Build user prompt with context and identity instructions
        val userPrompt = """
            ${languagePrefix}Context from our knowledge base:
            $contextText

            Question: ${context.message}

            $identityInstructions
        """.trimIndent()

        // Add messages to chat memory
        context.session.chatMemory.add(SystemMessage(systemPrompt))
        context.session.chatMemory.add(UserMessage(userPrompt))

        // Generate response
        val messages = context.session.chatMemory.messages()
        val aiResponse = responseGenerationService.generate(messages)

        val response = aiResponse?.text() ?: run {
            logger.error { "Failed to generate response with guardrails (identity with context)" }
            "I apologize, but I encountered an error. Please try again."
        }

        logger.info { "AI Response (identity with context): $response" }

        // Add AI response to memory
        context.session.chatMemory.add(AiMessage(response))

        return ChatResponse(
            response = response,
            sources = sources,
            needsConfidenceExtraction = true, // Identity responses include [CONFIDENCE: XX%]
            shouldLogAsUnanswered = false, // Identity questions shouldn't be logged as unanswered
            usedGeneralKnowledge = true
        )
    }

    /**
     * Format search results into context text
     */
    private fun formatContext(results: List<SearchResult>, showSources: Boolean): String {
        return results.joinToString("\n") { result ->
            val truncatedFact = result.fact.takeIf { it.length <= 500 }
                ?: (result.fact.take(500) + "...")

            result.source.takeIf { showSources }?.let { "- $truncatedFact [Source: $it]" }
                ?: "- $truncatedFact"
        }
    }
}
