package ai.sovereignrag.license.generation.command

import java.time.Instant

data class GenerateLicenseCommand(
    val clientId: String,
    val tier: String,
    val maxTokensPerMonth: Long,
    val maxKnowledgeBases: Int,
    val features: List<String>,
    val expiresAt: Instant?,
    val privateKey: String
)

data class GenerateLicenseResult(
    val success: Boolean,
    val licenseKey: String?,
    val clientId: String?,
    val tier: String?,
    val expiresAt: Instant?,
    val error: String?
)
