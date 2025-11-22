package ai.sovereignrag.core.chat.service

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ai.sovereignrag.chat.domain.ChatSession
import ai.sovereignrag.chat.dto.ChatInteractionResult
import ai.sovereignrag.chat.orchestrator.ChatResponseOrchestrator
import ai.sovereignrag.chat.strategy.ChatContext
import ai.sovereignrag.config.SovereignRagProperties
import ai.sovereignrag.content.service.ContentService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ConversationalAgentService(
    @Qualifier("guardrailChatModel") private val guardrailChatModel: ChatLanguageModel,
    private val contentService: ContentService,
    private val properties: SovereignRagProperties,
    private val promptTemplateService: ai.sovereignrag.prompt.service.PromptTemplateService,
    private val messageTranslationService: MessageTranslationService,
    private val orchestrator: ChatResponseOrchestrator
) {

    // Personas are now loaded from database via PersonaService
    // See database tables: prompt_templates, persona_configurations

    private fun getLanguageInstruction(language: String?, tenantId: String? = null): String {
        if (language == null || language.startsWith("en")) {
            return "" // No instruction needed for English
        }

        return try {
            val languageName = messageTranslationService.getLanguageName(language)
            val template = promptTemplateService.getTemplate(
                category = "system",
                name = "language_instruction",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: system/language_instruction")

            "\n\n" + promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf(
                    "language" to language,
                    "languageName" to languageName
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render language_instruction template, using fallback" }
            val languageName = messageTranslationService.getLanguageName(language)
            "\n\nCRITICAL: Respond in $languageName language. The user's interface is set to $languageName."
        }
    }

    private fun detectLanguage(text: String): String? {
        // Detect language from text using AI
        // Only detect if text is substantial enough (at least 10 characters)
        if (text.length < 10) {
            return null
        }

        return try {
            val prompt = "What language is this text written in? Reply with only the language name: \"$text\""
            // PERFORMANCE OPTIMIZATION: Use fast guardrail model (llama3.2:1b) for language detection
            // This is 3x faster than main model (llama3.2:3b) - saves ~0.8s per detection
            val result = guardrailChatModel.generate(UserMessage(prompt))
            val detectedLanguage = result.content().text().trim().lowercase()

            logger.info { "Detected language from text '$text': $detectedLanguage" }

            // Map language names to codes
            when {
                detectedLanguage.contains("dutch") || detectedLanguage.contains("nederlands") -> "nl"
                detectedLanguage.contains("german") || detectedLanguage.contains("deutsch") -> "de"
                detectedLanguage.contains("french") || detectedLanguage.contains("français") -> "fr"
                detectedLanguage.contains("spanish") || detectedLanguage.contains("español") -> "es"
                detectedLanguage.contains("italian") || detectedLanguage.contains("italiano") -> "it"
                detectedLanguage.contains("portuguese") || detectedLanguage.contains("português") -> "pt"
                detectedLanguage.contains("polish") || detectedLanguage.contains("polski") -> "pl"
                detectedLanguage.contains("russian") || detectedLanguage.contains("русский") -> "ru"
                detectedLanguage.contains("japanese") || detectedLanguage.contains("日本語") -> "ja"
                detectedLanguage.contains("chinese") || detectedLanguage.contains("中文") -> "zh"
                detectedLanguage.contains("korean") || detectedLanguage.contains("한국어") -> "ko"
                detectedLanguage.contains("arabic") || detectedLanguage.contains("العربية") -> "ar"
                detectedLanguage.contains("hindi") || detectedLanguage.contains("हिन्दी") -> "hi"
                detectedLanguage.contains("english") -> null // Return null for English (default)
                else -> {
                    logger.warn { "Could not map detected language '$detectedLanguage' to language code" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to detect language from text" }
            null
        }
    }

    /**
     * Process a chat interaction using the strategy pattern orchestrator
     *
     * This method has been refactored from a 485-line monolithic implementation
     * to use a clean Strategy Pattern with 7 specialized strategies.
     *
     * @param session The chat session
     * @param message User's message
     * @param useGeneralKnowledge Whether to allow general knowledge responses
     * @param showGkDisclaimer Whether to show general knowledge disclaimer
     * @param gkDisclaimerText Custom disclaimer text
     * @param showSources Whether to include source citations
     * @return ChatInteractionResult with response and metadata
     */
    fun processChatInteraction(
        session: ChatSession,
        message: String,
        useGeneralKnowledge: Boolean = true,
        showGkDisclaimer: Boolean = false,
        gkDisclaimerText: String? = null,
        showSources: Boolean = true
    ): ChatInteractionResult {
        logger.info { "Chat message in session ${session.sessionId}: $message" }

        // 1. Detect language
        val langDetectionStart = System.currentTimeMillis()
        val effectiveLanguage = if (!session.language.isNullOrBlank() && session.language != "auto") {
            session.language
        } else {
            detectLanguage(message)
        }
        val langDetectionTime = System.currentTimeMillis() - langDetectionStart
        logger.info { "Language: ${effectiveLanguage ?: "auto-detected"} (session.language: ${session.language}, took ${langDetectionTime}ms)" }

        // 2. Detect identity query
        val isIdentityQuery = isIdentityQuestion(message)
        if (isIdentityQuery) {
            logger.info { "Identity question detected - will prioritize identity response" }
        }

        // 3. Run parallel operations: language instruction + KB search
        logger.info { "Starting parallel operations: language instruction + KB search" }

        val (languageInstruction, rawSearchResults) = runBlocking {
            val languageInstructionDeferred = async {
                val instruction = getLanguageInstruction(effectiveLanguage)
                logger.info { "Generated language instruction: '${instruction.replace("\n", " ").trim()}'" }
                instruction
            }
            val searchDeferred = async {
                val searchStart = System.currentTimeMillis()
                val results = contentService.search(
                    message,
                    numResults = 5,
                    minConfidence = properties.knowledgeGraph.minConfidence,
                    language = effectiveLanguage
                )
                val searchTime = System.currentTimeMillis() - searchStart
                logger.info { "KB search completed in ${searchTime}ms" }
                results
            }

            Pair(languageInstructionDeferred.await(), searchDeferred.await())
        }

        // 4. Filter search results (remove manual-entry sources)
        val searchResults = rawSearchResults.map { result ->
            if (result.source?.startsWith("manual-entry-") == true) {
                result.copy(source = null)
            } else {
                result
            }
        }

        // 5. Determine hasHighQualityResults
        val hasHighQualityResults = searchResults.isNotEmpty() &&
            searchResults.first().confidence >= properties.knowledgeGraph.minConfidence

        logger.info {
            "Search results: ${searchResults.size} found | " +
            "Best: ${searchResults.firstOrNull()?.confidence?.let { String.format("%.4f", it) } ?: "N/A"}"
        }

        searchResults.take(3).forEachIndexed { index, result ->
            logger.info {
                "Result ${index + 1}: [${String.format("%.4f", result.confidence)}] " +
                "${result.fact.take(150)}... [Source: ${result.source}]"
            }
        }

        // 6. Build ChatContext for orchestrator
        val context = ChatContext(
            session = session,
            message = message,
            effectiveLanguage = effectiveLanguage,
            languageInstruction = languageInstruction,
            isIdentityQuery = isIdentityQuery,
            searchResults = searchResults,
            hasHighQualityResults = hasHighQualityResults,
            useGeneralKnowledge = useGeneralKnowledge,
            showGkDisclaimer = showGkDisclaimer,
            gkDisclaimerText = gkDisclaimerText,
            showSources = showSources,
            properties = properties
        )

        // 7. Delegate to orchestrator - this replaces 485 lines of if/else logic!
        return orchestrator.processInteraction(context)
    }

    // ===== Helper methods below are still used by other parts of the service =====

    /**
     * Detect identity questions (still used by processChatInteraction)
     */
    private fun isIdentityQuestion(message: String): Boolean {
        val classificationPrompt = """
            Determine if this message is asking about the chatbot/assistant's identity.

            Key distinction:
            - Identity questions ask about the CHATBOT itself (who/what the assistant is)
            - Non-identity questions ask about the ORGANIZATION/WEBSITE/PRODUCTS/SERVICES

            Examples:
            Identity: "Who are you?" "What are you?" "Introduce yourself"
            Non-identity: "Tell me about this website" "What services do you offer?" "Who is the CEO?"

            User message: "$message"

            Is this asking about the chatbot's identity?
            Answer ONLY "YES" or "NO".
        """.trimIndent()

        return try {
            val result = guardrailChatModel.generate(UserMessage(classificationPrompt))
            val answer = result.content().text().trim().uppercase()
            logger.debug { "Identity question classification for '$message': $answer" }
            answer == "YES"
        } catch (e: Exception) {
            logger.error(e) { "Error classifying identity question, defaulting to false" }
            false
        }
    }
}
