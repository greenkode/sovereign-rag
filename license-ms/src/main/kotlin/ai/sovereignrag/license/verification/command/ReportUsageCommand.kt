package ai.sovereignrag.license.verification.command

data class ReportUsageCommand(
    val licenseKey: String,
    val customerId: String,
    val deploymentId: String?,
    val tokensUsed: Long,
    val activeTenants: Int,
    val activeUsers: Int,
    val apiCalls: Long,
    val metadata: Map<String, Any>?
)

data class ReportUsageResult(
    val success: Boolean,
    val message: String
)
