package ai.sovereignrag.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.time.Duration

@ConfigurationProperties(prefix = "sovereignrag")
data class SovereignRagProperties @ConstructorBinding constructor(
    val ollama: OllamaProperties,
    val knowledgeGraph: KnowledgeGraphProperties,
    val chat: ChatProperties,
    val cors: CorsProperties,
    val admin: AdminProperties,
    val sendgrid: SendgridProperties
)

data class OllamaProperties(
    val baseUrl: String,
    val model: String,
    val embeddingModel: String,
    val guardrailModel: String = "llama3.2:1b",  // Faster model for quick threat classification
    val timeout: Duration = Duration.ofSeconds(120)
)

data class KnowledgeGraphProperties(
    val minConfidence: Double = 0.5,
    val lowConfidenceThreshold: Double = 0.5,
    val defaultNumResults: Int = 10,
    val maxResults: Int = 50,
    val useReranking: Boolean = true
)

data class ChatProperties(
    val enableRag: Boolean = true,
    val enableGeneralKnowledge: Boolean = true,
    val defaultPersona: String = "customer_service",
    val sessionTimeoutMinutes: Long = 30
)

data class CorsProperties(
    val allowedOrigins: String = "*",
    val allowedMethods: String = "GET,POST,PUT,DELETE,OPTIONS",
    val allowedHeaders: String = "*",
    val allowCredentials: Boolean = true
)

data class AdminProperties(
    val enabled: Boolean = true
)

data class SendgridProperties(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val fromEmail: String = "noreply@sovereignrag.ai",
    val fromName: String = "Sovereign RAG Support",
    val supportEmail: String = "support@sovereignrag.ai",
    val responseTime: String = "within 24 hours",
    val dashboardUrl: String = "http://localhost"
)
