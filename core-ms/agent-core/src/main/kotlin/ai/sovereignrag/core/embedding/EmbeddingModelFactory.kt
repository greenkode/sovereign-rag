package ai.sovereignrag.core.embedding

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Component
class EmbeddingModelFactory {
    private val modelCache = ConcurrentHashMap<String, EmbeddingModel>()

    fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        return modelCache.computeIfAbsent(config.id) {
            log.info { "Creating EmbeddingModel for ${config.name} (provider: ${config.provider})" }
            buildEmbeddingModel(config)
        }
    }

    private fun buildEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val provider = config.provider.lowercase()

        return when {
            isOpenAiCompatible(provider) -> buildOpenAiCompatibleModel(config)
            isOllama(provider) -> buildOllamaModel(config)
            else -> throw IllegalArgumentException("Unsupported embedding provider: ${config.provider}")
        }
    }

    private fun isOpenAiCompatible(provider: String): Boolean =
        provider in setOf("openai", "azure", "azure-openai", "together", "anyscale", "fireworks", "groq")

    private fun isOllama(provider: String): Boolean =
        provider in setOf("ollama", "ollama-cloud", "local")

    private fun buildOpenAiCompatibleModel(config: EmbeddingModelConfig): EmbeddingModel {
        val apiKey = config.apiKey
            ?: throw IllegalStateException("API key required for ${config.provider} embedding model: ${config.name}")

        val builder = OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(config.modelId)
            .timeout(Duration.ofMinutes(2))

        config.baseUrl?.let { builder.baseUrl(it) }
        config.dimensions.takeIf { it > 0 }?.let { builder.dimensions(it) }

        return builder.build().also {
            log.info { "Created OpenAI-compatible embedding model: ${config.name} (${config.modelId})" }
        }
    }

    private fun buildOllamaModel(config: EmbeddingModelConfig): EmbeddingModel {
        val baseUrl = config.baseUrl
            ?: throw IllegalStateException("Base URL required for ${config.provider} embedding model: ${config.name}")

        val builder = OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(config.modelId)
            .timeout(Duration.ofMinutes(2))

        config.apiKey?.takeIf { it.isNotBlank() }?.let {
            builder.customHeaders(mapOf("Authorization" to "Bearer $it"))
        }

        return builder.build().also {
            log.info { "Created Ollama embedding model: ${config.name} (${config.modelId})" }
        }
    }

    fun evictFromCache(modelId: String) {
        log.debug { "Evicting embedding model $modelId from cache" }
        modelCache.remove(modelId)
    }

    fun clearCache() {
        log.info { "Clearing embedding model cache" }
        modelCache.clear()
    }
}
