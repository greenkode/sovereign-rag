package ai.sovereignrag.knowledgebase.configuration.controller

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.commons.embedding.EmbeddingModelGateway
import ai.sovereignrag.commons.security.IsService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/embedding-models")
class InternalEmbeddingModelController(
    private val embeddingModelGateway: EmbeddingModelGateway
) {

    @GetMapping("/default")
    @IsService
    fun getDefaultModel(): EmbeddingModelConfigResponse {
        log.debug { "Internal API: Fetching default embedding model" }
        val model = embeddingModelGateway.getDefault()
        return model.toResponse()
    }

    @GetMapping("/{modelId}")
    @IsService
    fun getById(@PathVariable modelId: String): EmbeddingModelConfigResponse? {
        log.debug { "Internal API: Fetching embedding model by id: $modelId" }
        return embeddingModelGateway.findById(modelId)?.toResponse()
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    @IsService
    fun getByKnowledgeBase(@PathVariable knowledgeBaseId: UUID): EmbeddingModelConfigResponse? {
        log.debug { "Internal API: Fetching embedding model for knowledge base: $knowledgeBaseId" }
        return embeddingModelGateway.findByKnowledgeBase(knowledgeBaseId)?.toResponse()
    }

    private fun EmbeddingModelConfig.toResponse() = EmbeddingModelConfigResponse(
        id = id,
        name = name,
        modelId = modelId,
        provider = provider,
        dimensions = dimensions,
        maxTokens = maxTokens,
        baseUrl = baseUrl
    )
}

data class EmbeddingModelConfigResponse(
    val id: String,
    val name: String,
    val modelId: String,
    val provider: String,
    val dimensions: Int,
    val maxTokens: Int,
    val baseUrl: String?
)
