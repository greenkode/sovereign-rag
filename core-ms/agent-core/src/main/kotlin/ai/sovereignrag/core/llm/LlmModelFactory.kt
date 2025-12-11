package ai.sovereignrag.core.llm

import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Component
class LlmModelFactory(
    @Value("\${ollama.base-url:http://localhost:11434}") private val ollamaBaseUrl: String,
    @Value("\${openai.api-key:}") private val openAiApiKey: String,
    @Value("\${anthropic.api-key:}") private val anthropicApiKey: String
) {
    private val modelCache = ConcurrentHashMap<String, ChatLanguageModel>()
    private val streamingModelCache = ConcurrentHashMap<String, StreamingChatLanguageModel>()

    fun createChatModel(llmModel: LlmModel): ChatLanguageModel {
        return modelCache.computeIfAbsent(llmModel.id) {
            log.info { "Creating ChatLanguageModel for ${llmModel.name} (${llmModel.provider})" }
            when (llmModel.provider.lowercase()) {
                "ollama" -> createOllamaModel(llmModel)
                "openai" -> createOpenAiModel(llmModel)
                "anthropic" -> createAnthropicModel(llmModel)
                else -> throw IllegalArgumentException("Unsupported provider: ${llmModel.provider}")
            }
        }
    }

    fun createStreamingChatModel(llmModel: LlmModel): StreamingChatLanguageModel {
        return streamingModelCache.computeIfAbsent(llmModel.id) {
            log.info { "Creating StreamingChatLanguageModel for ${llmModel.name} (${llmModel.provider})" }
            when (llmModel.provider.lowercase()) {
                "ollama" -> createOllamaStreamingModel(llmModel)
                "openai" -> createOpenAiStreamingModel(llmModel)
                "anthropic" -> createAnthropicStreamingModel(llmModel)
                else -> throw IllegalArgumentException("Unsupported streaming provider: ${llmModel.provider}")
            }
        }
    }

    private fun createOllamaModel(llmModel: LlmModel): ChatLanguageModel {
        val baseUrl = llmModel.baseUrl ?: ollamaBaseUrl
        return OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(llmModel.modelId)
            .timeout(Duration.ofSeconds(120))
            .build()
    }

    private fun createOllamaStreamingModel(llmModel: LlmModel): StreamingChatLanguageModel {
        val baseUrl = llmModel.baseUrl ?: ollamaBaseUrl
        return OllamaStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(llmModel.modelId)
            .timeout(Duration.ofSeconds(120))
            .build()
    }

    private fun createOpenAiModel(llmModel: LlmModel): ChatLanguageModel {
        require(openAiApiKey.isNotBlank()) { "OpenAI API key is required for OpenAI models" }
        return OpenAiChatModel.builder()
            .apiKey(openAiApiKey)
            .modelName(llmModel.modelId)
            .maxTokens(llmModel.maxTokens)
            .timeout(Duration.ofSeconds(120))
            .build()
    }

    private fun createOpenAiStreamingModel(llmModel: LlmModel): StreamingChatLanguageModel {
        require(openAiApiKey.isNotBlank()) { "OpenAI API key is required for OpenAI streaming models" }
        return OpenAiStreamingChatModel.builder()
            .apiKey(openAiApiKey)
            .modelName(llmModel.modelId)
            .timeout(Duration.ofSeconds(120))
            .build()
    }

    private fun createAnthropicModel(llmModel: LlmModel): ChatLanguageModel {
        require(anthropicApiKey.isNotBlank()) { "Anthropic API key is required for Anthropic models" }
        return AnthropicChatModel.builder()
            .apiKey(anthropicApiKey)
            .modelName(llmModel.modelId)
            .maxTokens(llmModel.maxTokens)
            .timeout(Duration.ofSeconds(120))
            .build()
    }

    private fun createAnthropicStreamingModel(llmModel: LlmModel): StreamingChatLanguageModel {
        require(anthropicApiKey.isNotBlank()) { "Anthropic API key is required for Anthropic streaming models" }
        return AnthropicStreamingChatModel.builder()
            .apiKey(anthropicApiKey)
            .modelName(llmModel.modelId)
            .maxTokens(llmModel.maxTokens)
            .timeout(Duration.ofSeconds(120))
            .build()
    }

    fun clearCache() {
        log.info { "Clearing LLM model cache" }
        modelCache.clear()
        streamingModelCache.clear()
    }

    fun evictFromCache(modelId: String) {
        log.debug { "Evicting model $modelId from cache" }
        modelCache.remove(modelId)
        streamingModelCache.remove(modelId)
    }
}
