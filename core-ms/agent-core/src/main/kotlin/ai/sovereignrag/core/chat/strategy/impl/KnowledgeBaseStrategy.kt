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
 * Strategy for answering using knowledge base results
 *
 * This is the primary strategy for handling queries when high-quality KB results
 * are found. It formats the KB context, applies appropriate restrictions based on
 * whether general knowledge supplementation is allowed, and generates responses
 * that cite sources when configured.
 *
 * The strategy handles:
 * - Filtering and formatting relevant KB results
 * - Source citation management
 * - KB-only restriction when general knowledge is disabled
 * - Confidence extraction when KB confidence is below threshold
 *
 * This is the most complex strategy as it's the core of the KB-powered assistant.
 */
@Component
@Order(100) // Highest priority - KB results should be used when available
class KnowledgeBaseStrategy(
    private val responseGenerationService: ResponseGenerationService,
    private val messageTranslationService: MessageTranslationService,
    private val personaService: PersonaService,
    private val instructionService: PromptInstructionService
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // Can handle if we have high-quality KB results
        return context.hasHighQualityResults
    }

    override fun priority(): Int = 100

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.info { "Answering using knowledge base results: '${context.message}'" }

        // Filter relevant results based on top score
        val topScore = context.searchResults.first().confidence
        val relevantResults = topScore.takeIf { it >= 0.85 }
            ?.let { context.searchResults.filter { it.confidence >= (topScore - 0.3) } }
            ?: context.searchResults  // For lower confidence matches, use all results

        // Format context from relevant results
        val contextText = formatContext(relevantResults, context.showSources)

        // Extract sources
        val sources = relevantResults.mapNotNull { it.source }.distinct()

        val hasActualSources = sources.isNotEmpty()

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
                "includeSources" to if (context.showSources && hasActualSources) "- Include source links from the context at the end of your answer." else "",
                "sourceFormat" to if (context.showSources && hasActualSources) "[Source 1](URL1) | [Source 2](URL2)" else ""
            )
        )

        // Get source instructions
        val sourceInstructions = instructionService.getSourceInstructions(
            showSources = context.showSources,
            hasActualSources = hasActualSources,
            tenantId = null
        )

        // Get no sources warning if needed
        val noSourcesWarning = instructionService.getNoSourcesWarning(
            showSources = context.showSources,
            tenantId = null
        )

        // Determine if we need LLM confidence
        val maxKbConfidence = context.searchResults.maxOfOrNull { it.confidence } ?: 0.0
        val needsLlmConfidence = maxKbConfidence < context.properties.knowledgeGraph.minConfidence

        // Get confidence instruction if needed
        val confidenceInstruction = instructionService.getConfidenceInstruction(
            needsConfidence = needsLlmConfidence,
            tenantId = null
        )

        // Get KB-only restriction instruction if general knowledge is disabled
        val restrictionInstruction = instructionService.getRestrictionInstruction(
            useGeneralKnowledge = context.useGeneralKnowledge,
            tenantId = null
        )

        // Get answer format rules
        val answerFormatRules = instructionService.getAnswerFormatRules(
            confidenceInstruction = confidenceInstruction,
            tenantId = null
        )

        // Get language prefix
        val languagePrefix = instructionService.getLanguagePrefix(
            language = context.effectiveLanguage,
            getLanguageName = messageTranslationService::getLanguageName,
            tenantId = null
        )

        // Build system prompt
        val systemPrompt = personaPrompt + context.languageInstruction

        // Build user prompt
        val userPrompt = """
            ${languagePrefix}Based on the following information from our knowledge base:

            $contextText

            Question: ${context.message}
            $restrictionInstruction

            Provide a CONCISE, direct answer (1-2 sentences maximum).

            $sourceInstructions$noSourcesWarning

            $answerFormatRules
        """.trimIndent()

        logger.info { "System prompt (KB-based): ${systemPrompt.take(200)}..." }

        // Add messages to chat memory
        context.session.chatMemory.add(SystemMessage(systemPrompt))
        context.session.chatMemory.add(UserMessage(userPrompt))

        // Generate response
        val messages = context.session.chatMemory.messages()
        val aiResponse = responseGenerationService.generate(messages)

        val response = aiResponse?.text() ?: run {
            logger.error { "Failed to generate response with guardrails (knowledge base)" }
            "I apologize, but I encountered an error. Please try again."
        }

        logger.info { "AI Response (knowledge base): $response" }

        // Add AI response to memory
        context.session.chatMemory.add(AiMessage(response))

        return ChatResponse(
            response = response,
            sources = if (context.showSources) sources else emptyList(),
            needsConfidenceExtraction = needsLlmConfidence,
            shouldLogAsUnanswered = false, // KB-based responses shouldn't be logged as unanswered
            usedGeneralKnowledge = false
        )
    }

    /**
     * Format search results into context text
     */
    private fun formatContext(results: List<SearchResult>, showSources: Boolean): String {
        return results.joinToString("\n") { result ->
            val truncatedFact = result.fact.takeIf { it.length <= 500 }
                ?: (result.fact.take(500) + "...")

            val source = result.metadata["url"] as? String
            source.takeIf { showSources }?.let { "- $truncatedFact [Source: $it]" }
                ?: "- $truncatedFact"
        }
    }
}
