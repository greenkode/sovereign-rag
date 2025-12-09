package ai.sovereignrag.ingestion.core.embedding

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Service
class EmbeddingService(
    private val ingestionProperties: IngestionProperties
) {
    private val modelCache = ConcurrentHashMap<String, EmbeddingModel>()

    fun generateEmbeddings(texts: List<String>, modelConfig: EmbeddingModelConfig): List<FloatArray> {
        val embeddingModel = getModel(modelConfig) ?: createModel(modelConfig)
        val batchSize = ingestionProperties.embedding.batchSize
        val results = mutableListOf<FloatArray>()

        texts.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            log.debug { "Processing embedding batch ${batchIndex + 1}, size: ${batch.size}" }

            val response = embeddingModel.embedAll(batch.map { dev.langchain4j.data.segment.TextSegment.from(it) })

            response.content().forEach { embedding ->
                results.add(embedding.vector())
            }
        }

        return results
    }

    fun generateEmbedding(text: String, modelConfig: EmbeddingModelConfig): FloatArray {
        val embeddingModel = getModel(modelConfig) ?: createModel(modelConfig)
        val response = embeddingModel.embed(text)
        return response.content().vector()
    }

    private fun getModel(config: EmbeddingModelConfig): EmbeddingModel? {
        return modelCache[config.id]
    }

    private fun createModel(config: EmbeddingModelConfig): EmbeddingModel {
        val model = when (config.provider.lowercase()) {
            "openai" -> createOpenAiModel(config)
            "huggingface" -> createHuggingFaceModel(config)
            "ollama" -> createOllamaModel(config)
            else -> {
                log.warn { "Unknown provider ${config.provider}, falling back to Ollama" }
                createOllamaModel(config)
            }
        }
        return model.also { modelCache[config.id] = it }
    }

    private fun createOpenAiModel(config: EmbeddingModelConfig): EmbeddingModel {
        val apiKey = ingestionProperties.embedding.openaiApiKey
            ?: throw IllegalStateException("OpenAI API key not configured")

        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(config.modelId)
            .dimensions(config.dimensions)
            .timeout(Duration.ofMinutes(5))
            .build().also {
                log.info { "Created OpenAI embedding model: ${config.modelId}" }
            }
    }

    private fun createHuggingFaceModel(config: EmbeddingModelConfig): EmbeddingModel {
        val apiKey = ingestionProperties.embedding.huggingfaceApiKey
            ?: throw IllegalStateException("HuggingFace API key not configured")

        return HuggingFaceEmbeddingModel.builder()
            .accessToken(apiKey)
            .modelId(config.modelId)
            .timeout(Duration.ofMinutes(5))
            .build().also {
                log.info { "Created HuggingFace embedding model: ${config.modelId}" }
            }
    }

    private fun createOllamaModel(config: EmbeddingModelConfig): EmbeddingModel {
        val baseUrl = config.baseUrl ?: ingestionProperties.embedding.ollamaBaseUrl

        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(config.modelId)
            .timeout(Duration.ofMinutes(5))
            .build().also {
                log.info { "Created Ollama embedding model: ${config.modelId} at $baseUrl" }
            }
    }
}
