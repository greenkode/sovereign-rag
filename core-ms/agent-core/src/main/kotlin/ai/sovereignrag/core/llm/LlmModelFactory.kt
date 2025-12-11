package ai.sovereignrag.core.llm

import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Component
class LlmModelFactory {
    private val modelCache = ConcurrentHashMap<String, ChatLanguageModel>()
    private val streamingModelCache = ConcurrentHashMap<String, StreamingChatLanguageModel>()

    fun createChatModel(llmModel: LlmModel): ChatLanguageModel {
        return modelCache.computeIfAbsent(llmModel.id) {
            log.info { "Creating ChatLanguageModel for ${llmModel.name} (provider: ${llmModel.provider})" }
            buildChatModel(llmModel)
        }
    }

    fun createStreamingChatModel(llmModel: LlmModel): StreamingChatLanguageModel {
        return streamingModelCache.computeIfAbsent(llmModel.id) {
            log.info { "Creating StreamingChatLanguageModel for ${llmModel.name} (provider: ${llmModel.provider})" }
            buildStreamingChatModel(llmModel)
        }
    }

    private fun buildChatModel(model: LlmModel): ChatLanguageModel {
        val provider = model.provider.lowercase()

        return when {
            isOpenAiCompatible(provider) -> buildOpenAiCompatibleChatModel(model)
            isAnthropicCompatible(provider) -> buildAnthropicChatModel(model)
            isOllama(provider) -> buildOllamaChatModel(model)
            else -> throw IllegalArgumentException("Unsupported LLM provider: ${model.provider}")
        }
    }

    private fun buildStreamingChatModel(model: LlmModel): StreamingChatLanguageModel {
        val provider = model.provider.lowercase()

        return when {
            isOpenAiCompatible(provider) -> buildOpenAiCompatibleStreamingModel(model)
            isAnthropicCompatible(provider) -> buildAnthropicStreamingModel(model)
            isOllama(provider) -> buildOllamaStreamingModel(model)
            else -> throw IllegalArgumentException("Unsupported streaming LLM provider: ${model.provider}")
        }
    }

    private fun isOpenAiCompatible(provider: String): Boolean =
        provider in setOf("openai", "azure", "azure-openai", "together", "anyscale", "fireworks", "groq", "deepseek")

    private fun isAnthropicCompatible(provider: String): Boolean =
        provider in setOf("anthropic", "claude")

    private fun isOllama(provider: String): Boolean =
        provider in setOf("ollama", "ollama-cloud", "local")

    private fun buildOpenAiCompatibleChatModel(model: LlmModel): ChatLanguageModel {
        val apiKey = model.apiKey
            ?: throw IllegalStateException("API key required for ${model.provider} model: ${model.name}")

        val builder = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(model.modelId)
            .maxTokens(model.maxTokens)
            .timeout(Duration.ofSeconds(120))

        model.baseUrl?.let { builder.baseUrl(it) }

        return builder.build().also {
            log.info { "Created OpenAI-compatible chat model: ${model.name} (${model.modelId})" }
        }
    }

    private fun buildOpenAiCompatibleStreamingModel(model: LlmModel): StreamingChatLanguageModel {
        val apiKey = model.apiKey
            ?: throw IllegalStateException("API key required for ${model.provider} streaming model: ${model.name}")

        val builder = OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName(model.modelId)
            .timeout(Duration.ofSeconds(120))

        model.baseUrl?.let { builder.baseUrl(it) }

        return builder.build().also {
            log.info { "Created OpenAI-compatible streaming model: ${model.name} (${model.modelId})" }
        }
    }

    private fun buildAnthropicChatModel(model: LlmModel): ChatLanguageModel {
        val apiKey = model.apiKey
            ?: throw IllegalStateException("API key required for ${model.provider} model: ${model.name}")

        val builder = AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName(model.modelId)
            .maxTokens(model.maxTokens)
            .timeout(Duration.ofSeconds(120))

        model.baseUrl?.let { builder.baseUrl(it) }

        return builder.build().also {
            log.info { "Created Anthropic chat model: ${model.name} (${model.modelId})" }
        }
    }

    private fun buildAnthropicStreamingModel(model: LlmModel): StreamingChatLanguageModel {
        val apiKey = model.apiKey
            ?: throw IllegalStateException("API key required for ${model.provider} streaming model: ${model.name}")

        val builder = AnthropicStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName(model.modelId)
            .maxTokens(model.maxTokens)
            .timeout(Duration.ofSeconds(120))

        model.baseUrl?.let { builder.baseUrl(it) }

        return builder.build().also {
            log.info { "Created Anthropic streaming model: ${model.name} (${model.modelId})" }
        }
    }

    private fun buildOllamaChatModel(model: LlmModel): ChatLanguageModel {
        val baseUrl = model.baseUrl
            ?: throw IllegalStateException("Base URL required for ${model.provider} model: ${model.name}")

        val builder = OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(model.modelId)
            .timeout(Duration.ofSeconds(120))

        model.apiKey?.takeIf { it.isNotBlank() }?.let {
            builder.customHeaders(mapOf("Authorization" to "Bearer $it"))
        }

        return builder.build().also {
            log.info { "Created Ollama chat model: ${model.name} (${model.modelId})" }
        }
    }

    private fun buildOllamaStreamingModel(model: LlmModel): StreamingChatLanguageModel {
        val baseUrl = model.baseUrl
            ?: throw IllegalStateException("Base URL required for ${model.provider} streaming model: ${model.name}")

        val builder = OllamaStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(model.modelId)
            .timeout(Duration.ofSeconds(120))

        model.apiKey?.takeIf { it.isNotBlank() }?.let {
            builder.customHeaders(mapOf("Authorization" to "Bearer $it"))
        }

        return builder.build().also {
            log.info { "Created Ollama streaming model: ${model.name} (${model.modelId})" }
        }
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
