package ai.sovereignrag.core.prompt.repository

import nl.compilot.ai.prompt.domain.PersonaConfiguration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for persona configuration storage and retrieval
 *
 * Supports tenant-specific persona overrides
 */
@Repository
interface PersonaConfigurationRepository : JpaRepository<PersonaConfiguration, Long> {

    /**
     * Find persona by key with tenant override support
     *
     * Returns tenant-specific persona if exists, otherwise global persona
     * Strategy: ORDER BY tenant_id DESC NULLS LAST ensures tenant-specific comes first
     */
    @Query("""
        SELECT pc FROM PersonaConfiguration pc
        WHERE pc.personaKey = :personaKey
          AND pc.active = true
          AND (pc.tenantId = :tenantId OR pc.tenantId IS NULL)
        ORDER BY pc.tenantId DESC NULLS LAST
    """)
    fun findByPersonaKeyWithTenantOverride(
        @Param("personaKey") personaKey: String,
        @Param("tenantId") tenantId: String?
    ): List<PersonaConfiguration>

    /**
     * Find all active personas for a tenant (including global)
     */
    @Query("""
        SELECT pc FROM PersonaConfiguration pc
        WHERE pc.active = true
          AND (pc.tenantId = :tenantId OR pc.tenantId IS NULL)
        ORDER BY pc.displayName
    """)
    fun findActivePersonasForTenant(@Param("tenantId") tenantId: String?): List<PersonaConfiguration>

    /**
     * Find persona by exact tenant and key match
     */
    fun findByPersonaKeyAndTenantId(personaKey: String, tenantId: String?): PersonaConfiguration?

    /**
     * Find all global personas (tenant_id IS NULL)
     */
    fun findByTenantIdIsNullAndActiveTrue(): List<PersonaConfiguration>

    /**
     * Find all tenant-specific personas
     */
    fun findByTenantIdAndActiveTrue(tenantId: String): List<PersonaConfiguration>

    /**
     * Check if persona exists for tenant
     */
    fun existsByPersonaKeyAndTenantId(personaKey: String, tenantId: String?): Boolean
}
