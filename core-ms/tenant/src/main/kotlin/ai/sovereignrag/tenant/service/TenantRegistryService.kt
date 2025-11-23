package ai.sovereignrag.tenant.service

import io.github.oshai.kotlinlogging.KotlinLogging
import ai.sovereignrag.tenant.domain.Tenant
import ai.sovereignrag.commons.tenant.TenantStatus
import ai.sovereignrag.commons.tenant.TenantNotFoundException
import ai.sovereignrag.tenant.repository.TenantRepository
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.sql.DriverManager
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Tenant Registry Service - manages tenant lifecycle
 * Uses JPA repository for tenant CRUD operations
 * Uses raw JDBC for DDL operations (CREATE/DROP DATABASE)
 */
@Service
@Transactional(transactionManager = "masterTransactionManager")
class TenantRegistryService(
    private val tenantRepository: TenantRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.host:localhost}") private val dbHost: String,
    @Value("\${spring.datasource.port:5432}") private val dbPort: Int
) : TenantRegistry {

    private val secureRandom = SecureRandom()

    /**
     * Get tenant by ID (cached)
     */
    @Cacheable(cacheNames = ["tenants"], key = "#tenantId")
    override fun getTenant(tenantId: String): Tenant {
        return tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
            ?: throw TenantNotFoundException("Tenant not found: $tenantId")
    }

    /**
     * Validate tenant credentials
     * Uses PasswordEncoder for constant-time comparison to prevent timing attacks
     */
    override fun validateTenant(tenantId: String, apiKey: String): Tenant? {
        return try {
            val tenant = getTenant(tenantId)

            // Use passwordEncoder.matches() for secure, constant-time comparison
            if (passwordEncoder.matches(apiKey, tenant.apiKeyHash)) {
                tenant
            } else {
                logger.warn { "Invalid API key for tenant: $tenantId" }
                null
            }
        } catch (e: TenantNotFoundException) {
            logger.warn { "Tenant not found: $tenantId" }
            null
        }
    }

    /**
     * Create new tenant with isolated database
     */
    @CacheEvict(cacheNames = ["tenants"], allEntries = true)
    fun createTenant(
        tenantId: String,
        name: String,
        contactEmail: String? = null,
        contactName: String? = null,
        wordpressUrl: String? = null
    ): CreateTenantResult {
        logger.info { "Creating new tenant: $tenantId ($name)" }

        val databaseName = "sovereignrag_tenant_${tenantId.lowercase().replace("-", "_")}"
        val apiKey = generateApiKey()
        val apiKeyHash = hashApiKey(apiKey)

        try {
            // 1. Create PostgreSQL database
            createPostgreSQLDatabase(databaseName)

            // 2. Apply schema migrations to new database
            applySchemaToDatabase(databaseName)

            // 3. Register tenant in master database using repository
            val tenant = Tenant(
                id = tenantId,
                name = name,
                databaseName = databaseName,
                apiKeyHash = apiKeyHash,
                status = TenantStatus.ACTIVE,
                contactEmail = contactEmail,
                contactName = contactName,
                wordpressUrl = wordpressUrl
            )
            tenantRepository.save(tenant)

            logger.info { "Tenant created successfully: $tenantId" }

            return CreateTenantResult(
                tenant = getTenant(tenantId),
                apiKey = apiKey
            )

        } catch (e: Exception) {
            logger.error(e) { "Failed to create tenant: $tenantId" }
            // Cleanup on failure
            try {
                dropDatabase(databaseName)
            } catch (cleanupEx: Exception) {
                logger.error(cleanupEx) { "Failed to cleanup database: $databaseName" }
            }
            throw TenantCreationException("Failed to create tenant: ${e.message}", e)
        }
    }

    /**
     * Update tenant's last active timestamp
     */
    override fun updateLastActive(tenantId: String) {
        tenantRepository.updateLastActive(tenantId, Instant.now())
    }

    /**
     * List all tenants
     */
    fun listTenants(status: TenantStatus? = null): List<Tenant> {
        return if (status != null) {
            tenantRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status)
        } else {
            tenantRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
        }
    }

    /**
     * Regenerate API key for a tenant
     */
    @CacheEvict(cacheNames = ["tenants"], key = "#tenantId")
    fun regenerateApiKey(tenantId: String): RegenerateApiKeyResult {
        logger.info { "Regenerating API key for tenant: $tenantId" }

        val newApiKey = generateApiKey()
        val newApiKeyHash = hashApiKey(newApiKey)

        // Update API key hash (DelegatingPasswordEncoder adds {bcrypt} prefix automatically)
        val tenant = getTenant(tenantId)
        tenant.apiKeyHash = newApiKeyHash
        tenant.updatedAt = Instant.now()
        tenantRepository.save(tenant)

        logger.info { "API key regenerated successfully for tenant: $tenantId" }

        return RegenerateApiKeyResult(
            tenantId = tenantId,
            newApiKey = newApiKey
        )
    }

    /**
     * Delete tenant and its database (soft delete)
     */
    @CacheEvict(cacheNames = ["tenants"], key = "#tenantId")
    fun deleteTenant(tenantId: String, hardDelete: Boolean = false) {
        val tenant = getTenant(tenantId)

        if (hardDelete) {
            logger.warn { "HARD DELETE tenant: $tenantId" }
            // Drop the database (DDL operation - must use raw JDBC)
            dropDatabase(tenant.databaseName)

            // Delete from registry
            tenantRepository.deleteById(tenantId)
        } else {
            logger.info { "Soft delete tenant: $tenantId" }
            // Soft delete (mark as deleted)
            val now = Instant.now()
            tenantRepository.softDelete(tenantId, now, now)
        }
    }

    /**
     * Apply pending migrations to all tenant databases
     * Useful when new migrations are added and need to be applied to existing tenants
     */
    fun migrateAllTenants() {
        logger.info { "Applying pending migrations to all tenant databases..." }

        val tenants = listTenants()
        var successCount = 0
        var failureCount = 0

        for (tenant in tenants) {
            try {
                logger.info { "Migrating tenant: ${tenant.id} (${tenant.databaseName})" }
                applySchemaToDatabase(tenant.databaseName)
                successCount++
            } catch (e: Exception) {
                logger.error(e) { "Failed to migrate tenant: ${tenant.id}" }
                failureCount++
            }
        }

        logger.info { "Migration complete: $successCount succeeded, $failureCount failed" }
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private fun createPostgreSQLDatabase(databaseName: String) {
        logger.info { "Creating PostgreSQL database: $databaseName" }

        val serverUrl = "jdbc:postgresql://$dbHost:$dbPort/postgres"
        DriverManager.getConnection(serverUrl, dbUsername, dbPassword).use { conn ->
            conn.autoCommit = true
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE DATABASE $databaseName
                    WITH OWNER = $dbUsername
                         ENCODING = 'UTF8'
                         LC_COLLATE = 'en_US.UTF-8'
                         LC_CTYPE = 'en_US.UTF-8'
                         TEMPLATE = template0
                """)
            }
        }

        // Enable extensions
        val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$databaseName"
        DriverManager.getConnection(dbUrl, dbUsername, dbPassword).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS btree_gin")
            }
        }

        logger.info { "Database created with extensions: $databaseName" }
    }

    private fun applySchemaToDatabase(databaseName: String) {
        logger.info { "Applying schema migrations to: $databaseName" }

        val flyway = Flyway.configure()
            .dataSource(
                "jdbc:postgresql://$dbHost:$dbPort/$databaseName",
                dbUsername,
                dbPassword
            )
            .locations("classpath:db/tenant-schema")
            .baselineOnMigrate(true)
            .placeholderReplacement(false)  // Disable to preserve ${} in templates
            .load()

        val result = flyway.migrate()
        logger.info { "Applied ${result.migrationsExecuted} migrations to $databaseName" }
    }

    private fun dropDatabase(databaseName: String) {
        logger.warn { "Dropping database: $databaseName" }

        val serverUrl = "jdbc:postgresql://$dbHost:$dbPort/postgres"
        DriverManager.getConnection(serverUrl, dbUsername, dbPassword).use { conn ->
            conn.autoCommit = true

            // Terminate all connections to the database
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    SELECT pg_terminate_backend(pg_stat_activity.pid)
                    FROM pg_stat_activity
                    WHERE pg_stat_activity.datname = '$databaseName'
                      AND pid <> pg_backend_pid()
                """)
            }

            // Drop the database
            conn.createStatement().use { stmt ->
                stmt.execute("DROP DATABASE IF EXISTS $databaseName")
            }
        }
    }

    private fun generateApiKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Hash API key using BCrypt password encoder
     * BCrypt provides:
     * - Built-in salting (no manual salt management needed)
     * - Configurable work factor (computational cost)
     * - Resistance to brute-force attacks
     */
    private fun hashApiKey(apiKey: String): String {
        return passwordEncoder.encode(apiKey)
    }
}

/**
 * Result of tenant creation
 */
data class CreateTenantResult(
    val tenant: Tenant,
    val apiKey: String  // Only returned once!
)

/**
 * Result of API key regeneration
 */
data class RegenerateApiKeyResult(
    val tenantId: String,
    val newApiKey: String  // Only returned once!
)

class TenantCreationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
