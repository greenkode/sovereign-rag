package ai.sovereignrag.ingestion.core.config

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConditionalOnProperty(prefix = "ingestion.remi", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RemiLlmConfig(
    private val ingestionProperties: IngestionProperties
) {

    @Bean("remiChatModel")
    fun remiChatModel(): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(ingestionProperties.remi.ollamaBaseUrl)
            .modelName(ingestionProperties.remi.modelName)
            .timeout(Duration.ofSeconds(ingestionProperties.remi.timeoutSeconds))
            .build()
    }
}
