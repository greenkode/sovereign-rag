package ai.sovereignrag.identity.core.profile.service

import dev.langchain4j.model.chat.ChatLanguageModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class PromptRefinementService(
    private val chatLanguageModel: ChatLanguageModel?
) {

    fun refinePrompt(previousPrompts: List<String>, newRequest: String): String {
        if (previousPrompts.isEmpty()) {
            return newRequest
        }

        val model = chatLanguageModel ?: run {
            log.warn { "ChatLanguageModel not configured, using simple prompt concatenation" }
            return buildSimpleRefinedPrompt(previousPrompts, newRequest)
        }

        val systemPrompt = """
            You are an AI assistant that helps refine avatar image generation prompts.
            Given a history of previous prompts and a new user request, create a single cohesive prompt
            that incorporates the refinements while maintaining the core characteristics from previous prompts.

            Rules:
            1. The output should be a single, clear image generation prompt
            2. Preserve important details from previous prompts unless explicitly changed
            3. Apply the new modifications or additions requested
            4. Keep the prompt concise but descriptive (max 200 words)
            5. Output ONLY the refined prompt, no explanations or prefixes
        """.trimIndent()

        val conversationHistory = previousPrompts.mapIndexed { index, prompt ->
            "Prompt ${index + 1}: $prompt"
        }.joinToString("\n")

        val userMessage = """
            Previous prompts:
            $conversationHistory

            New request: $newRequest

            Generate a refined, cohesive prompt that incorporates the new request while maintaining relevant details from previous prompts.
        """.trimIndent()

        return runCatching {
            val response = model.generate("$systemPrompt\n\n$userMessage")
            log.info { "Refined prompt generated successfully" }
            response.trim()
        }.getOrElse { e ->
            log.error(e) { "Failed to refine prompt using LLM, falling back to simple concatenation" }
            buildSimpleRefinedPrompt(previousPrompts, newRequest)
        }
    }

    private fun buildSimpleRefinedPrompt(previousPrompts: List<String>, newRequest: String): String {
        val lastPrompt = previousPrompts.lastOrNull() ?: return newRequest
        return "$lastPrompt. Additionally: $newRequest"
    }
}
