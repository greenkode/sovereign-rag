package ai.sovereignrag.core.chat.service.instruction

import mu.KotlinLogging
import nl.compilot.ai.prompt.service.PromptTemplateService
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service for loading and rendering instruction templates
 *
 * Centralizes all instruction template access to reduce duplication
 * and provide consistent template loading with fallbacks.
 */
@Service
class PromptInstructionService(
    private val promptTemplateService: PromptTemplateService
) {

    /**
     * Get source citation instructions based on configuration
     */
    fun getSourceInstructions(showSources: Boolean, hasActualSources: Boolean, tenantId: String? = null): String {
        return try {
            val templateName = when {
                showSources && hasActualSources -> "source_include"
                !showSources -> "source_user_disabled"
                else -> "source_not_available"
            }

            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = templateName,
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/$templateName")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = emptyMap()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render source instructions, using fallback" }
            when {
                showSources && hasActualSources -> "Include the source links from the context at the end of your answer."
                !showSources -> "IMPORTANT: Do NOT include any source links, citations, or references in your answer.\nSimply provide the answer without any [Source N](URL) links."
                else -> "IMPORTANT: The context above has NO source URLs. Do NOT add any links to your answer."
            }
        }
    }

    /**
     * Get warning when user has disabled sources
     */
    fun getNoSourcesWarning(showSources: Boolean, tenantId: String? = null): String {
        if (showSources) return ""

        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "no_sources_warning",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/no_sources_warning")

            "\n\n" + promptTemplateService.renderTemplate(
                template = template,
                parameters = emptyMap()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render no_sources_warning template, using fallback" }
            "\n\nWARNING: DO NOT INCLUDE [Source N](URL) LINKS IN YOUR ANSWER. The user has disabled source citations."
        }
    }

    /**
     * Get confidence instruction when LLM needs to provide confidence score
     */
    fun getConfidenceInstruction(needsConfidence: Boolean, tenantId: String? = null): String {
        if (!needsConfidence) return ""

        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "confidence_instruction",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/confidence_instruction")

            "\n\n" + promptTemplateService.renderTemplate(
                template = template,
                parameters = emptyMap()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render confidence_instruction template, using fallback" }
            """

After your response, on a new line, add your confidence level in the format:
[CONFIDENCE: XX%]
where XX is a NUMBER from 0 to 100 representing your confidence percentage.
Example: [CONFIDENCE: 85%]
"""
        }
    }

    /**
     * Get language prefix for user prompts
     */
    fun getLanguagePrefix(language: String?, getLanguageName: (String) -> String, tenantId: String? = null): String {
        if (language.isNullOrBlank() || language.startsWith("en")) return ""

        return try {
            val languageName = getLanguageName(language)
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "language_prompt_prefix",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/language_prompt_prefix")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf("languageName" to languageName)
            ) + "\n\n"
        } catch (e: Exception) {
            logger.error(e) { "Failed to render language_prompt_prefix template, using fallback" }
            val languageName = getLanguageName(language)
            "IMPORTANT: Answer in $languageName language.\n\n"
        }
    }

    /**
     * Get KB-only restriction instruction
     */
    fun getRestrictionInstruction(useGeneralKnowledge: Boolean, tenantId: String? = null): String {
        if (useGeneralKnowledge) return ""

        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "kb_only_restriction",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/kb_only_restriction")

            "\n\n" + promptTemplateService.renderTemplate(
                template = template,
                parameters = emptyMap()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render kb_only_restriction template, using fallback" }
            """

CRITICAL RESTRICTION:
- You MUST answer ONLY based on the information provided above
- Do NOT use any external knowledge or information not in the context
- If the provided information doesn't fully answer the question, simply state what you know from the context
- Do NOT supplement with information from Wikipedia, news sources, or any other external source
- Only provide information that is explicitly stated in the context above
- Do NOT add commentary like "no further information available" or "that's all we know"
"""
        }
    }

    /**
     * Get answer format rules
     */
    fun getAnswerFormatRules(confidenceInstruction: String, tenantId: String? = null): String {
        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "answer_format_rules",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/answer_format_rules")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf("confidenceInstruction" to confidenceInstruction)
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render answer_format_rules template, using fallback" }
            """
CRITICAL RULES - Answer Format:
- Answer the question directly and concisely
- Do NOT add phrases like "no further information available" or "that's all we know"
- Do NOT speculate about missing information
- Just answer what was asked using the facts provided$confidenceInstruction
"""
        }
    }

    /**
     * Get identity question instructions (with context)
     */
    fun getIdentityWithContextInstructions(showSources: Boolean, sources: List<String>, tenantId: String? = null): String {
        val sourceInstruction = if (showSources && sources.isNotEmpty()) {
            "- Only cite sources if referring to specific About/identity info"
        } else {
            ""
        }

        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "identity_with_context",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/identity_with_context")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf("sourceInstruction" to sourceInstruction)
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render identity_with_context template, using fallback" }
            """
This is an identity/introduction question. Answer it briefly and directly:
- First, introduce yourself as described in your system prompt (AI assistant for this website)
- Then, if the context contains relevant "About" or organizational information, mention it briefly
- Do NOT include irrelevant content about other topics
- Keep response concise (2-3 sentences maximum)

IMPORTANT:
- Focus ONLY on identity and "About" information
- Do NOT mix identity response with unrelated website content
- Answer naturally without referencing "the text" or "knowledge base"
$sourceInstruction
"""
        }
    }

    /**
     * Get identity question instructions (no context)
     */
    fun getIdentityNoContextInstructions(tenantId: String? = null): String {
        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "identity_no_context",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/identity_no_context")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = emptyMap()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render identity_no_context template, using fallback" }
            """
This is an identity/introduction question. Answer it briefly and directly:
- Introduce yourself as described in your system prompt
- Be concise (1-2 sentences)
- Do NOT include any other topics or website content

After your response, on a new line, add your confidence level in the format:
[CONFIDENCE: XX%]
"""
        }
    }

    /**
     * Get general knowledge instructions (with context)
     */
    fun getGeneralKnowledgeWithContextInstructions(
        showSources: Boolean,
        sources: List<String>,
        disclaimerInstruction: String,
        tenantId: String? = null
    ): String {
        val sourceInstruction = if (showSources && sources.isNotEmpty()) {
            "- Cite sources using the format [Source: url]"
        } else {
            ""
        }

        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "general_knowledge_with_context",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/general_knowledge_with_context")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf(
                    "sourceInstruction" to sourceInstruction,
                    "disclaimerInstruction" to disclaimerInstruction
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render general_knowledge_with_context template, using fallback" }
            """
- If the context is not sufficient, supplement with your general knowledge
- Provide a helpful, concise answer (2-3 sentences)

IMPORTANT:
- Do NOT say "the text does not mention..." or "the provided text..."
- Answer the question directly as if you're having a normal conversation
$sourceInstruction
$disclaimerInstruction
"""
        }
    }

    /**
     * Get general knowledge instructions (no context)
     */
    fun getGeneralKnowledgeNoContextInstructions(disclaimerInstruction: String, tenantId: String? = null): String {
        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "general_knowledge_no_context",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/general_knowledge_no_context")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf("disclaimerInstruction" to disclaimerInstruction)
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render general_knowledge_no_context template, using fallback" }
            """
Answer this question directly using your general knowledge.
Provide a helpful, concise answer (2-3 sentences).

IMPORTANT:
- Do NOT say "the text does not mention..." or "the provided text..."
- Do NOT reference any knowledge base, documents, or sources
- Answer the question directly as if you're having a normal conversation
$disclaimerInstruction
"""
        }
    }

    /**
     * Get conversation history instructions
     */
    fun getConversationHistoryInstructions(noResultsMessage: String, tenantId: String? = null): String {
        return try {
            val template = promptTemplateService.getTemplate(
                category = "instruction",
                name = "conversation_history",
                tenantId = tenantId
            ) ?: throw IllegalArgumentException("Template not found: instruction/conversation_history")

            promptTemplateService.renderTemplate(
                template = template,
                parameters = mapOf("noResultsMessage" to noResultsMessage)
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to render conversation_history template, using fallback" }
            """
IMPORTANT: Only answer if you can find the information in our previous conversation.
If you cannot answer from our conversation history, say: "$noResultsMessage"
"""
        }
    }
}
