package ai.sovereignrag.knowledgebase.configuration.integration

import ai.sovereignrag.commons.embedding.EmbeddingModelNotFoundException
import ai.sovereignrag.knowledgebase.config.AbstractIntegrationTest
import ai.sovereignrag.knowledgebase.configuration.domain.EmbeddingModel
import ai.sovereignrag.knowledgebase.configuration.repository.EmbeddingModelRepository
import ai.sovereignrag.knowledgebase.configuration.service.EmbeddingModelService
import ai.sovereignrag.knowledgebase.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Import(EmbeddingModelService::class)
class EmbeddingModelServiceIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var embeddingModelService: EmbeddingModelService

    @Autowired
    private lateinit var embeddingModelRepository: EmbeddingModelRepository

    @Autowired
    private lateinit var knowledgeBaseRepository: KnowledgeBaseRepository

    private val testModels = mutableListOf<EmbeddingModel>()
    private val testKnowledgeBases = mutableListOf<KnowledgeBase>()

    @BeforeEach
    fun setup() {
        val openaiModel = embeddingModelRepository.save(
            EmbeddingModel(
                id = "test-openai-model",
                name = "Test OpenAI Model",
                modelId = "text-embedding-3-small",
                description = "Test model for integration tests",
                provider = "openai",
                dimensions = 1536,
                maxTokens = 8191,
                baseUrl = "https://api.openai.com/v1",
                enabled = true,
                sortOrder = 1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        testModels.add(openaiModel)

        val ollamaModel = embeddingModelRepository.save(
            EmbeddingModel(
                id = "test-ollama-model",
                name = "Test Ollama Model",
                modelId = "nomic-embed-text",
                description = "Test Ollama model",
                provider = "ollama",
                dimensions = 768,
                maxTokens = 512,
                baseUrl = "http://localhost:11434",
                enabled = true,
                sortOrder = 2,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        testModels.add(ollamaModel)
    }

    @AfterEach
    fun cleanup() {
        testKnowledgeBases.forEach { knowledgeBaseRepository.delete(it) }
        testKnowledgeBases.clear()
        testModels.forEach { embeddingModelRepository.delete(it) }
        testModels.clear()
    }

    @Test
    fun `findById should return model from database`() {
        val result = embeddingModelService.findById("test-openai-model")

        assertNotNull(result)
        assertEquals("test-openai-model", result.id)
        assertEquals("openai", result.provider)
        assertEquals(1536, result.dimensions)
        assertEquals("https://api.openai.com/v1", result.baseUrl)
    }

    @Test
    fun `findById should return null for non-existent model`() {
        val result = embeddingModelService.findById("non-existent-model")

        assertNull(result)
    }

    @Test
    fun `findByKnowledgeBase should return configured model`() {
        val kb = createAndSaveKnowledgeBase("test-openai-model")

        val result = embeddingModelService.findByKnowledgeBase(UUID.fromString(kb.id))

        assertNotNull(result)
        assertEquals("test-openai-model", result.id)
        assertEquals("openai", result.provider)
    }

    @Test
    fun `findByKnowledgeBase should return default when KB has no model configured`() {
        val kb = createAndSaveKnowledgeBase(null)

        val result = embeddingModelService.findByKnowledgeBase(UUID.fromString(kb.id))

        assertNotNull(result)
        assertEquals("test-openai-model", result.id)
    }

    @Test
    fun `findByKnowledgeBase should return null when KB not found`() {
        val result = embeddingModelService.findByKnowledgeBase(UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `getDefault should return first enabled model by sort order`() {
        val result = embeddingModelService.getDefault()

        assertEquals("test-openai-model", result.id)
    }

    @Test
    fun `getDefault should throw when no models available`() {
        testModels.forEach { embeddingModelRepository.delete(it) }
        testModels.clear()

        assertThrows<EmbeddingModelNotFoundException> {
            embeddingModelService.getDefault()
        }
    }

    private fun createAndSaveKnowledgeBase(embeddingModelId: String?): KnowledgeBase {
        val id = UUID.randomUUID()
        val kb = knowledgeBaseRepository.save(
            KnowledgeBase(
                id = id.toString(),
                name = "Test KB",
                organizationId = UUID.randomUUID(),
                schemaName = "kb_${id.toString().replace("-", "_").take(32)}",
                regionCode = "eu-west",
                embeddingModelId = embeddingModelId,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        testKnowledgeBases.add(kb)
        return kb
    }
}
