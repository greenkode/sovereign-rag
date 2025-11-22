package ai.sovereignrag.core.chat.service

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * Service for translating standard messages to different languages
 *
 * Provides translation for common system messages like "no results",
 * disclaimers, and follow-up questions.
 */
@Service
class MessageTranslationService(
    private val chatModel: ChatLanguageModel
) {

    /**
     * Get language name from language code using Java's Locale
     */
    fun getLanguageName(languageCode: String): String {
        return try {
            val locale = Locale.forLanguageTag(languageCode)
            locale.getDisplayLanguage(Locale.ENGLISH).takeIf { it.isNotBlank() }
                ?: "the user's language"
        } catch (_: Exception) {
            logger.warn { "Could not determine language name for code: $languageCode" }
            "the user's language"
        }
    }

    /**
     * Translate "no results" message to the specified language
     */
    fun translateNoResultsMessage(language: String?): String {
        if (language == null) {
            return "I don't have information about that in our knowledge base."
        }

        return try {
            val languageName = getLanguageName(language)
            val englishMessage = "I don't have information about that in our knowledge base."
            val prompt = "Translate the following text to $languageName. Return ONLY the translated text, with no explanation or additional commentary:\n\n\"$englishMessage\""
            val result = chatModel.generate(UserMessage(prompt))
            result.content().text().trim()
        } catch (e: Exception) {
            logger.error(e) { "Failed to translate no-results message to $language" }
            "I don't have information about that in our knowledge base."
        }
    }

    /**
     * Translate general knowledge disclaimer to the specified language
     */
    fun translateDisclaimer(language: String?): String {
        if (language == null) {
            return "*Note: This is general knowledge, not from our knowledge base.*"
        }

        return try {
            val languageName = getLanguageName(language)
            val englishDisclaimer = "Note: This is general knowledge, not from our knowledge base."
            val prompt = "Translate the following text to $languageName. Return ONLY the translated text, with no explanation or additional commentary:\n\n\"$englishDisclaimer\""
            val result = chatModel.generate(UserMessage(prompt))
            val translated = result.content().text().trim()
            "*$translated*"  // Add italic formatting
        } catch (e: Exception) {
            logger.error(e) { "Failed to translate disclaimer to $language" }
            "*Note: This is general knowledge, not from our knowledge base.*"
        }
    }

    /**
     * Translate follow-up question to the specified language
     */
    fun translateFollowUpQuestion(language: String?): String {
        if (language == null) {
            return "Is there anything else I can help you with?"
        }

        return try {
            val languageName = getLanguageName(language)
            val englishMessage = "Is there anything else I can help you with?"
            val prompt = "Translate the following text to $languageName. Return ONLY the translated text, with no explanation or additional commentary:\n\n\"$englishMessage\""
            val result = chatModel.generate(UserMessage(prompt))
            result.content().text().trim()
        } catch (e: Exception) {
            logger.error(e) { "Failed to translate follow-up question to $language" }
            "Is there anything else I can help you with?"
        }
    }
}
