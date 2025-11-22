package ai.sovereignrag.commons.tenant

/**
 * Tenant information interface
 * Provides essential tenant data for authentication and resource routing
 */
interface TenantInfo {
    val id: String
    val apiKeyHash: String
    val status: TenantStatus
    val databaseName: String  // For tenant-specific database routing
}

/**
 * Tenant status
 */
enum class TenantStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
