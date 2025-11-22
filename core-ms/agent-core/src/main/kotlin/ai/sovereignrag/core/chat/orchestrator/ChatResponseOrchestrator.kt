package ai.sovereignrag.core.chat.orchestrator

import mu.KotlinLogging
import ai.sovereignrag.chat.dto.ChatInteractionResult
import ai.sovereignrag.chat.service.MessageTranslationService
import ai.sovereignrag.chat.service.extraction.ConfidenceExtractor
import ai.sovereignrag.chat.service.extraction.EscalationDetector
import ai.sovereignrag.chat.strategy.ChatContext
import ai.sovereignrag.chat.strategy.ChatResponse
import ai.sovereignrag.chat.strategy.ChatResponseStrategy
import ai.sovereignrag.commons.UnansweredQueryLogger
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Orchestrator for chat response generation
 *
 * Coordinates multiple response strategies to handle different types of chat
 * interactions. Selects the appropriate strategy based on context, generates
 * the response, and applies post-processing (confidence extraction, escalation
 * detection).
 *
 * This is the main entry point for generating chat responses in the refactored
 * architecture, replacing the monolithic processChatInteraction method.
 */
@Component
class ChatResponseOrchestrator(
    private val strategies: List<ChatResponseStrategy>,
    private val confidenceExtractor: ConfidenceExtractor,
    private val escalationDetector: EscalationDetector,
    private val messageTranslationService: MessageTranslationService,
    private val unansweredQueryService: UnansweredQueryLogger
) {

    init {
        logger.info { "ChatResponseOrchestrator initialized with ${strategies.size} strategies:" }
        strategies.sortedByDescending { it.priority() }.forEach { strategy ->
            logger.info { "  - ${strategy.javaClass.simpleName} (priority: ${strategy.priority()})" }
        }
    }

    /**
     * Process a chat interaction and generate a response
     *
     * @param context Complete context for the chat interaction
     * @return ChatInteractionResult with response and metadata
     */
    fun processInteraction(context: ChatContext): ChatInteractionResult {
        logger.info { "Processing chat interaction for message: '${context.message}'" }

        // Find all strategies that can handle this context
        val candidateStrategies = strategies.filter { it.canHandle(context) }

        if (candidateStrategies.isEmpty()) {
            logger.error { "No strategy found to handle context! This should never happen." }
            return ChatInteractionResult(
                response = "I apologize, but I encountered an error processing your request.",
                sources = emptyList(),
                userSeemsFinished = false,
                confidenceScore = null,
                showConfidence = false
            )
        }

        // Select highest priority strategy
        val selectedStrategy = candidateStrategies.maxByOrNull { it.priority() }!!
        logger.info { "Selected strategy: ${selectedStrategy.javaClass.simpleName} (priority: ${selectedStrategy.priority()})" }

        // Generate response using selected strategy
        var chatResponse = selectedStrategy.generateResponse(context)

        // Post-process: Extract confidence if needed
        chatResponse.takeIf { it.needsConfidenceExtraction }?.let {
            val (cleanedResponse, extractedConfidence) = confidenceExtractor.extract(it.response)
            val updatedResponse = it.copy(response = cleanedResponse)

            extractedConfidence?.let { confidence ->
                logger.info { "Extracted LLM confidence: $confidence%" }
            }

            // Convert to result with confidence
            return processWithConfidence(updatedResponse, extractedConfidence, context)
        }

        // Post-process: Detect and handle escalation
        val followUpMessage = messageTranslationService.translateFollowUpQuestion(context.effectiveLanguage)
        val escalationResult = escalationDetector.detectAndProcess(
            response = chatResponse.response,
            session = context.session,
            followUpMessage = followUpMessage
        )

        chatResponse = escalationResult
            .takeIf { it.wasEscalation }
            ?.processedResponse
            ?.also { logger.info { "Escalation detected and processed" } }
            ?.let { chatResponse.copy(response = it) }
            ?: chatResponse

        // Log as unanswered if strategy indicated
        chatResponse.takeIf { it.shouldLogAsUnanswered }?.let {
            unansweredQueryService.logUnansweredQuery(
                query = context.message,
                response = it.response,
                bestConfidence = context.searchResults.firstOrNull()?.confidence,
                usedGeneralKnowledge = it.usedGeneralKnowledge
            )
        }

        // Determine if we should show confidence
        // Show confidence if we have high-quality KB results
        val showConfidence = context.hasHighQualityResults

        return ChatInteractionResult(
            response = chatResponse.response,
            sources = chatResponse.sources,
            userSeemsFinished = false, // Legacy field, always false now
            confidenceScore = null, // No LLM confidence extracted for this path
            showConfidence = showConfidence
        )
    }

    /**
     * Process response that included confidence extraction
     */
    private fun processWithConfidence(
        chatResponse: ChatResponse,
        extractedConfidence: Int?,
        context: ChatContext
    ): ChatInteractionResult {
        // Post-process: Detect and handle escalation
        val followUpMessage = messageTranslationService.translateFollowUpQuestion(context.effectiveLanguage)
        val escalationResult = escalationDetector.detectAndProcess(
            response = chatResponse.response,
            session = context.session,
            followUpMessage = followUpMessage
        )

        val finalResponse = escalationResult
            .takeIf { it.wasEscalation }
            ?.processedResponse
            ?.also { logger.info { "Escalation detected and processed" } }
            ?: chatResponse.response

        // Log as unanswered if strategy indicated
        chatResponse.takeIf { it.shouldLogAsUnanswered }?.let {
            unansweredQueryService.logUnansweredQuery(
                query = context.message,
                response = finalResponse,
                bestConfidence = context.searchResults.firstOrNull()?.confidence,
                usedGeneralKnowledge = it.usedGeneralKnowledge
            )
        }

        // Determine if we should show confidence
        // Show confidence if we have high-quality KB results OR extracted LLM confidence
        val showConfidence = context.hasHighQualityResults || extractedConfidence != null

        return ChatInteractionResult(
            response = finalResponse,
            sources = chatResponse.sources,
            userSeemsFinished = false, // Legacy field, always false now
            confidenceScore = extractedConfidence,
            showConfidence = showConfidence
        )
    }
}
