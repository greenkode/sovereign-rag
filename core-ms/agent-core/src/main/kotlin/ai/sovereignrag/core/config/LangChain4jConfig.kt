package ai.sovereignrag.core.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Duration

/**
 * LangChain4j configuration for LLM and embedding models
 * Now uses PostgreSQL + pgvector for document storage (no Neo4j)
 */
@Configuration
class LangChain4jConfig(
    private val properties: CompilotProperties
) {

    /**
     * Chat language model for generating responses
     * Uses Ollama with llama3.2:3b by default
     */
    @Bean
    @Primary
    fun chatLanguageModel(): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(properties.ollama.baseUrl)
            .modelName(properties.ollama.model)
            .timeout(properties.ollama.timeout)
            .build()
    }

    /**
     * Guardrail chat model for semantic threat detection
     * Uses lightweight llama3.2:1b for 3x faster threat classification
     * Injected with qualifier "guardrailChatModel" for guardrails
     */
    @Bean("guardrailChatModel")
    fun guardrailChatModel(): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(properties.ollama.baseUrl)
            .modelName(properties.ollama.guardrailModel)  // Use faster 1B model
            .timeout(Duration.ofSeconds(10))              // Shorter timeout for quick classification
            .build()
    }

    /**
     * Embedding model for generating vector embeddings
     * Uses Ollama with snowflake-arctic-embed2 (1024 dimensions)
     */
    @Bean
    fun embeddingModel(): EmbeddingModel {
        return OllamaEmbeddingModel.builder()
            .baseUrl(properties.ollama.baseUrl)
            .modelName(properties.ollama.embeddingModel)
            .timeout(Duration.ofSeconds(60))
            .build()
    }
}
