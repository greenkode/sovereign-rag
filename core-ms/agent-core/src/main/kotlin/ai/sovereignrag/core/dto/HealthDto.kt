package ai.sovereignrag.core.dto

data class HealthResponse(
    val status: String,
    val database: String,
    val llm: String,
    val embedding: String
)
