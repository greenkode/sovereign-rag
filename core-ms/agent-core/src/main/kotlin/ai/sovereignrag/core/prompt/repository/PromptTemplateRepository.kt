package ai.sovereignrag.core.prompt.repository

import nl.compilot.ai.prompt.domain.PromptTemplate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for prompt template storage and retrieval
 *
 * Supports tenant-specific overrides via ORDER BY tenant_id DESC NULLS LAST
 */
@Repository
interface PromptTemplateRepository : JpaRepository<PromptTemplate, Long> {

    /**
     * Find template by category and name with tenant override support
     *
     * Returns tenant-specific template if exists, otherwise global template
     * Strategy: ORDER BY tenant_id DESC NULLS LAST ensures tenant-specific comes first
     */
    @Query("""
        SELECT pt FROM PromptTemplate pt
        WHERE pt.category = :category
          AND pt.name = :name
          AND pt.active = true
          AND (pt.tenantId = :tenantId OR pt.tenantId IS NULL)
        ORDER BY pt.tenantId DESC NULLS LAST, pt.version DESC
    """)
    fun findByCategoryAndNameWithTenantOverride(
        @Param("category") category: String,
        @Param("name") name: String,
        @Param("tenantId") tenantId: String?
    ): List<PromptTemplate>

    /**
     * Find all active templates for a specific category
     */
    fun findByCategoryAndActiveTrueOrderByNameAsc(category: String): List<PromptTemplate>

    /**
     * Find all active templates for a tenant (including global)
     */
    @Query("""
        SELECT pt FROM PromptTemplate pt
        WHERE pt.active = true
          AND (pt.tenantId = :tenantId OR pt.tenantId IS NULL)
        ORDER BY pt.category, pt.name, pt.tenantId DESC NULLS LAST
    """)
    fun findActiveTemplatesForTenant(@Param("tenantId") tenantId: String?): List<PromptTemplate>

    /**
     * Find templates by ID list (for persona template lookups)
     */
    fun findByIdIn(ids: List<Long>): List<PromptTemplate>

    /**
     * Check if template exists for tenant
     */
    fun existsByCategoryAndNameAndTenantId(
        category: String,
        name: String,
        tenantId: String?
    ): Boolean
}
