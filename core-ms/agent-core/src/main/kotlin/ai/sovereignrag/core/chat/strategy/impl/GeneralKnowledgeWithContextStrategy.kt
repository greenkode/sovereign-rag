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
import ai.sovereignrag.domain.SearchResult
import ai.sovereignrag.prompt.service.PersonaService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Strategy for answering using general knowledge WITH KB context
 *
 * When general knowledge is enabled and there ARE KB results (but may not be
 * high quality or complete), answer using BOTH the KB context and general knowledge.
 * The LLM can supplement incomplete KB information with its general knowledge.
 *
 * A disclaimer is shown if configured to indicate the response includes general
 * knowledge rather than only KB information.
 *
 * This strategy requires confidence extraction as the LLM is asked to
 * provide a confidence score with its response.
 */
@Component
@Order(75) // Medium-high priority - between no context GK and identity strategies
class GeneralKnowledgeWithContextStrategy(
    private val responseGenerationService: ResponseGenerationService,
    private val messageTranslationService: MessageTranslationService,
    private val personaService: PersonaService,
    private val instructionService: PromptInstructionService
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // Can handle if:
        // - General knowledge is enabled
        // - We have some KB results (even if not high quality)
        // Note: Also handles identity queries by customizing the prompt
        return context.useGeneralKnowledge &&
                context.searchResults.isNotEmpty()
    }

    override fun priority(): Int = 75

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.info { "Answering using general knowledge with context: '${context.message}'" }

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
            val identityInstructions = instructionService.getIdentityWithContextInstructions(
                showSources = context.showSources,
                sources = sources,
                tenantId = null
            )
            """
                ${languagePrefix}Context from our knowledge base:
                $contextText

                Question: ${context.message}

                $identityInstructions
            """.trimIndent()
        } else {
            // Normal general knowledge with context
            val disclaimerInstruction = context.gkDisclaimerText
                .takeIf { context.showGkDisclaimer }
                ?.let { "\n\nIMPORTANT: Add this disclaimer at the end of your response:\n$it" }
                ?: ""

            val generalKnowledgeInstructions = instructionService.getGeneralKnowledgeWithContextInstructions(
                showSources = context.showSources,
                sources = sources,
                disclaimerInstruction = disclaimerInstruction,
                tenantId = null
            )

            """
                ${languagePrefix}Context from our knowledge base (possibly relevant):
                $contextText

                Question: ${context.message}

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
            logger.error { "Failed to generate response with guardrails (general knowledge with context)" }
            "I apologize, but I encountered an error. Please try again."
        }

        logger.info { "AI Response (general knowledge with context): $response" }

        // Add AI response to memory
        context.session.chatMemory.add(AiMessage(response))

        return ChatResponse(
            response = response,
            sources = sources,
            needsConfidenceExtraction = true, // General knowledge responses include [CONFIDENCE: XX%]
            shouldLogAsUnanswered = false, // General knowledge responses shouldn't be logged as unanswered
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
