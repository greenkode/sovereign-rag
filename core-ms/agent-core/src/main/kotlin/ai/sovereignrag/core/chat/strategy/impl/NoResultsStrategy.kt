package ai.sovereignrag.core.chat.strategy.impl

import mu.KotlinLogging
import ai.sovereignrag.chat.service.MessageTranslationService
import ai.sovereignrag.chat.strategy.ChatContext
import ai.sovereignrag.chat.strategy.ChatResponse
import ai.sovereignrag.chat.strategy.ChatResponseStrategy
import ai.sovereignrag.commons.UnansweredQueryLogger
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Strategy for handling queries with no KB results and general knowledge disabled
 * with no conversation history available.
 *
 * Returns a "no results" message in the appropriate language.
 * This is the fallback strategy with lowest priority.
 */
@Component
@Order(100) // Lowest priority - fallback strategy
class NoResultsStrategy(
    private val messageTranslationService: MessageTranslationService,
    private val unansweredQueryService: UnansweredQueryLogger
) : ChatResponseStrategy {

    override fun canHandle(context: ChatContext): Boolean {
        // This is the fallback strategy - can always handle
        // Will be selected if no other strategy matches
        return !context.hasHighQualityResults &&
                !context.useGeneralKnowledge &&
                context.session.chatMemory.messages().isEmpty()
    }

    override fun priority(): Int = 0 // Lowest priority

    override fun generateResponse(context: ChatContext): ChatResponse {
        logger.warn { "No high-quality results and no conversation history for query: '${context.message}'" }

        // Use the translateNoResultsMessage method from MessageTranslationService
        val response = messageTranslationService.translateNoResultsMessage(context.effectiveLanguage)

        // Log this as an unanswered query
        unansweredQueryService.logUnansweredQuery(
            query = context.message,
            response = response,
            bestConfidence = context.searchResults.firstOrNull()?.confidence,
            usedGeneralKnowledge = false
        )

        return ChatResponse(
            response = response,
            sources = emptyList(),
            needsConfidenceExtraction = false,
            shouldLogAsUnanswered = false, // Already logged above
            usedGeneralKnowledge = false
        )
    }
}
