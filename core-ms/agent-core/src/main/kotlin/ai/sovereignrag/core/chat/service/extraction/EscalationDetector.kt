package ai.sovereignrag.core.chat.service.extraction

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import mu.KotlinLogging
import ai.sovereignrag.chat.domain.ChatSession
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service for detecting and processing escalation requests
 *
 * Handles detection of escalation triggers and extraction of
 * user contact information for escalation emails.
 */
@Service
class EscalationDetector(
    private val chatModel: ChatLanguageModel,
    private val emailTool: ai.sovereignrag.tools.EmailTool
) {

    companion object {
        private const val ESCALATION_KEYWORD = "__ESCALATION_READY__"
    }

    /**
     * Escalation information extracted from conversation
     */
    data class EscalationInfo(
        val email: String,
        val name: String,
        val message: String,
        val phone: String? = null
    )

    /**
     * Result of escalation detection and processing
     */
    data class EscalationResult(
        val wasEscalation: Boolean,
        val processedResponse: String? = null, // Replacement response if escalation occurred
        val escalationInfo: EscalationInfo? = null,
        val error: String? = null
    )

    /**
     * Detect if response contains escalation trigger and process if present
     *
     * @param response AI response text to check for escalation
     * @param session Chat session containing conversation history
     * @param followUpMessage Message to append after successful escalation
     * @return EscalationResult indicating if escalation was detected and processed
     */
    fun detectAndProcess(
        response: String,
        session: ChatSession,
        followUpMessage: String
    ): EscalationResult {
        return response.takeIf { it.contains(ESCALATION_KEYWORD) }
            ?.let { processEscalation(session, followUpMessage) }
            ?: EscalationResult(wasEscalation = false)
    }

    private fun processEscalation(session: ChatSession, followUpMessage: String): EscalationResult {
        logger.info { "Escalation ready detected, extracting information from conversation" }

        val escalationInfo = extractEscalationInfo(session) ?: run {
            logger.warn { "Escalation ready but could not extract information" }
            return EscalationResult(
                wasEscalation = true,
                processedResponse = "I apologize, but I couldn't collect all the necessary information. Please make sure to provide your email, name, and message.",
                error = "Failed to extract escalation info"
            )
        }

        return try {
            val emailResult = emailTool.sendEscalationEmail(
                userEmail = escalationInfo.email,
                userName = escalationInfo.name,
                userMessage = escalationInfo.message,
                userPhone = escalationInfo.phone ?: "",
                sessionId = session.sessionId
            )

            // Replace the entire response with the email tool result + follow-up
            // This prevents the __ESCALATION_READY__ keyword from showing to the user
            val processedResponse = "$emailResult\n\n$followUpMessage"

            logger.info { "Escalation email sent successfully" }

            EscalationResult(
                wasEscalation = true,
                processedResponse = processedResponse,
                escalationInfo = escalationInfo
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to send escalation email" }
            EscalationResult(
                wasEscalation = true,
                processedResponse = "I apologize, but I encountered an error while submitting your request. Please try contacting our support team directly.",
                escalationInfo = escalationInfo,
                error = e.message
            )
        }
    }

    /**
     * Extract escalation information from conversation history using AI
     *
     * @param session Chat session containing conversation history
     * @return EscalationInfo if all required fields extracted, null otherwise
     */
    private fun extractEscalationInfo(session: ChatSession): EscalationInfo? {
        return try {
            // Get conversation history
            val conversationHistory = session.chatMemory.messages()
                .joinToString("\n") { message ->
                    when (message) {
                        is UserMessage -> "USER: ${message.singleText()}"
                        is AiMessage -> "ASSISTANT: ${message.text()}"
                        else -> ""
                    }
                }

            // Use AI to extract the information
            val extractionPrompt = """
                Extract the following information from this conversation where a user is requesting human support:

                $conversationHistory

                Extract and return ONLY the following in this exact JSON format:
                {
                  "email": "user's email address",
                  "name": "user's full name",
                  "phone": "user's phone number or null if not provided",
                  "message": "user's detailed message/issue description"
                }

                Return ONLY the JSON, no other text.
            """.trimIndent()

            val result = chatModel.generate(UserMessage(extractionPrompt))
            val jsonText = result.content().text().trim()

            // Simple JSON parsing
            val emailRegex = """"email"\s*:\s*"([^"]+)"""".toRegex()
            val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
            val phoneRegex = """"phone"\s*:\s*"([^"]+)"""".toRegex()
            val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()

            val email = emailRegex.find(jsonText)?.groupValues?.get(1)
            val name = nameRegex.find(jsonText)?.groupValues?.get(1)
            val phone = phoneRegex.find(jsonText)?.groupValues?.get(1)?.takeIf { it != "null" }
            val message = messageRegex.find(jsonText)?.groupValues?.get(1)

            takeIf { email != null && name != null && message != null }
                ?.let { EscalationInfo(email!!, name!!, message!!, phone) }
                ?: run {
                    logger.warn { "Could not extract all required information. Email: $email, Name: $name, Message: ${message != null}" }
                    null
                }
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract escalation info" }
            null
        }
    }
}
