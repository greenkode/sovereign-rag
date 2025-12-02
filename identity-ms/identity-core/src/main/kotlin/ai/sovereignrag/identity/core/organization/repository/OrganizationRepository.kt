package ai.sovereignrag.identity.core.organization.repository

import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.organization.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationRepository : JpaRepository<Organization, UUID> {

    fun findBySlug(slug: String): Organization?

    fun findByIdAndStatus(id: UUID, status: OrganizationStatus): Organization?

    fun existsBySlug(slug: String): Boolean

    fun findAllByStatus(status: OrganizationStatus): List<Organization>
}
