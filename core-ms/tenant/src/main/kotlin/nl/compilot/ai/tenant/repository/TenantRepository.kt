package nl.compilot.ai.tenant.repository

import nl.compilot.ai.tenant.domain.Tenant
import nl.compilot.ai.commons.tenant.TenantStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * JPA Repository for managing tenants in the master database
 * All queries operate on master.tenants table
 */
@Repository
interface TenantRepository : JpaRepository<Tenant, String> {

    /**
     * Find tenant by ID excluding soft-deleted tenants
     */
    fun findByIdAndDeletedAtIsNull(id: String): Tenant?

    /**
     * Find all active tenants (not soft-deleted)
     */
    fun findByDeletedAtIsNullOrderByCreatedAtDesc(): List<Tenant>

    /**
     * Find all tenants by status (excluding soft-deleted)
     */
    fun findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status: TenantStatus): List<Tenant>

    /**
     * Check if tenant exists by ID (excluding soft-deleted)
     */
    fun existsByIdAndDeletedAtIsNull(id: String): Boolean

    /**
     * Check if database name already exists
     */
    fun existsByDatabaseName(databaseName: String): Boolean

    /**
     * Update last active timestamp
     */
    @Modifying
    @Query("""
        UPDATE Tenant t
        SET t.lastActiveAt = :timestamp
        WHERE t.id = :tenantId
    """)
    fun updateLastActive(tenantId: String, timestamp: Instant)

    /**
     * Update API key hash
     */
    @Modifying
    @Query("""
        UPDATE Tenant t
        SET t.apiKeyHash = :apiKeyHash,
            t.updatedAt = :updatedAt
        WHERE t.id = :tenantId
    """)
    fun updateApiKeyHash(tenantId: String, apiKeyHash: String, updatedAt: Instant)

    /**
     * Soft delete tenant
     */
    @Modifying
    @Query("""
        UPDATE Tenant t
        SET t.status = 'DELETED',
            t.deletedAt = :deletedAt,
            t.updatedAt = :updatedAt
        WHERE t.id = :tenantId
    """)
    fun softDelete(tenantId: String, deletedAt: Instant, updatedAt: Instant)
}
