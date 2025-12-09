package ai.sovereignrag.knowledgebase.configuration.unit

import ai.sovereignrag.commons.embedding.EmbeddingModelNotFoundException
import ai.sovereignrag.knowledgebase.configuration.domain.EmbeddingModel
import ai.sovereignrag.knowledgebase.configuration.repository.EmbeddingModelRepository
import ai.sovereignrag.knowledgebase.configuration.service.EmbeddingModelService
import ai.sovereignrag.knowledgebase.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmbeddingModelServiceTest {

    private val embeddingModelRepository: EmbeddingModelRepository = mockk()
    private val knowledgeBaseRepository: KnowledgeBaseRepository = mockk()

    private lateinit var service: EmbeddingModelService

    @BeforeEach
    fun setup() {
        service = EmbeddingModelService(embeddingModelRepository, knowledgeBaseRepository)
    }

    @Test
    fun `findById should return model when found`() {
        val modelId = "openai-text-3-small"
        val model = createMockEmbeddingModel(modelId, "OpenAI Text Small", "openai")

        every { embeddingModelRepository.findById(modelId) } returns Optional.of(model)

        val result = service.findById(modelId)

        assertNotNull(result)
        assertEquals(modelId, result.id)
        assertEquals("openai", result.provider)
        verify { embeddingModelRepository.findById(modelId) }
    }

    @Test
    fun `findById should return null when model not found`() {
        val modelId = "non-existent-model"

        every { embeddingModelRepository.findById(modelId) } returns Optional.empty()

        val result = service.findById(modelId)

        assertNull(result)
    }

    @Test
    fun `findByKnowledgeBase should return configured model when KB has embeddingModelId`() {
        val knowledgeBaseId = UUID.randomUUID()
        val modelId = "openai-text-3-large"
        val knowledgeBase = createMockKnowledgeBase(knowledgeBaseId, modelId)
        val model = createMockEmbeddingModel(modelId, "OpenAI Text Large", "openai")

        every { knowledgeBaseRepository.findById(knowledgeBaseId.toString()) } returns Optional.of(knowledgeBase)
        every { embeddingModelRepository.findById(modelId) } returns Optional.of(model)

        val result = service.findByKnowledgeBase(knowledgeBaseId)

        assertNotNull(result)
        assertEquals(modelId, result.id)
        assertEquals("openai", result.provider)
    }

    @Test
    fun `findByKnowledgeBase should return default model when KB has no embeddingModelId`() {
        val knowledgeBaseId = UUID.randomUUID()
        val knowledgeBase = createMockKnowledgeBase(knowledgeBaseId, null)
        val defaultModel = createMockEmbeddingModel("default-model", "Default Model", "ollama")

        every { knowledgeBaseRepository.findById(knowledgeBaseId.toString()) } returns Optional.of(knowledgeBase)
        every { embeddingModelRepository.findByEnabledTrueOrderBySortOrder() } returns listOf(defaultModel)

        val result = service.findByKnowledgeBase(knowledgeBaseId)

        assertNotNull(result)
        assertEquals("default-model", result.id)
    }

    @Test
    fun `findByKnowledgeBase should return null when KB not found`() {
        val knowledgeBaseId = UUID.randomUUID()

        every { knowledgeBaseRepository.findById(knowledgeBaseId.toString()) } returns Optional.empty()

        val result = service.findByKnowledgeBase(knowledgeBaseId)

        assertNull(result)
    }

    @Test
    fun `findByKnowledgeBase should return default when configured model not found`() {
        val knowledgeBaseId = UUID.randomUUID()
        val modelId = "deleted-model"
        val knowledgeBase = createMockKnowledgeBase(knowledgeBaseId, modelId)
        val defaultModel = createMockEmbeddingModel("default-model", "Default Model", "ollama")

        every { knowledgeBaseRepository.findById(knowledgeBaseId.toString()) } returns Optional.of(knowledgeBase)
        every { embeddingModelRepository.findById(modelId) } returns Optional.empty()
        every { embeddingModelRepository.findByEnabledTrueOrderBySortOrder() } returns listOf(defaultModel)

        val result = service.findByKnowledgeBase(knowledgeBaseId)

        assertNotNull(result)
        assertEquals("default-model", result.id)
    }

    @Test
    fun `getDefault should return first enabled model`() {
        val models = listOf(
            createMockEmbeddingModel("first-model", "First Model", "openai"),
            createMockEmbeddingModel("second-model", "Second Model", "huggingface")
        )

        every { embeddingModelRepository.findByEnabledTrueOrderBySortOrder() } returns models

        val result = service.getDefault()

        assertEquals("first-model", result.id)
    }

    @Test
    fun `getDefault should throw exception when no models available`() {
        every { embeddingModelRepository.findByEnabledTrueOrderBySortOrder() } returns emptyList()

        assertThrows<EmbeddingModelNotFoundException> {
            service.getDefault()
        }
    }

    private fun createMockEmbeddingModel(
        id: String,
        name: String,
        provider: String,
        dimensions: Int = 1536,
        maxTokens: Int = 8191,
        baseUrl: String? = null
    ): EmbeddingModel {
        return EmbeddingModel(
            id = id,
            name = name,
            modelId = id,
            description = "Test model $name",
            provider = provider,
            dimensions = dimensions,
            maxTokens = maxTokens,
            baseUrl = baseUrl,
            enabled = true,
            sortOrder = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun createMockKnowledgeBase(
        id: UUID,
        embeddingModelId: String?
    ): KnowledgeBase {
        return KnowledgeBase(
            id = id.toString(),
            name = "Test KB",
            organizationId = UUID.randomUUID(),
            schemaName = "kb_test",
            regionCode = "eu-west",
            embeddingModelId = embeddingModelId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
