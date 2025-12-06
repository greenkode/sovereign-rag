package ai.sovereignrag.identity.core.deletion.service

import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.organization.entity.Organization
import ai.sovereignrag.identity.core.organization.repository.OrganizationRepository
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class OrganizationDeletionService(
    private val organizationRepository: OrganizationRepository,
    private val userDeletionService: UserDeletionService,
    private val registeredClientRepository: OAuthRegisteredClientRepository
) {

    @Transactional
    fun deleteOrganization(organizationId: UUID): OrganizationDeletionResult {
        val organization = organizationRepository.findById(organizationId).orElse(null)
            ?: return OrganizationDeletionResult(
                success = false,
                message = "error.organization_not_found",
                organizationId = organizationId,
                usersDeleted = 0
            )

        if (organization.status == OrganizationStatus.DELETED) {
            return OrganizationDeletionResult(
                success = false,
                message = "error.organization_already_deleted",
                organizationId = organizationId,
                usersDeleted = 0
            )
        }

        log.info { "Starting GDPR-compliant deletion for organization: $organizationId" }

        val usersDeleted = userDeletionService.deleteUsersByOrganizationId(organizationId)
        softDeleteRegisteredClients(organizationId)
        anonymizeAndMarkDeleted(organization)

        log.info { "Completed GDPR-compliant deletion for organization: $organizationId, users deleted: $usersDeleted" }

        return OrganizationDeletionResult(
            success = true,
            message = "organization.deleted_successfully",
            organizationId = organizationId,
            usersDeleted = usersDeleted
        )
    }

    private fun softDeleteRegisteredClients(organizationId: UUID) {
        val clients = registeredClientRepository.findByOrganizationId(organizationId)
        clients.forEach { client ->
            client.status = OrganizationStatus.DELETED
            client.clientName = "${ANONYMIZED_CLIENT_PREFIX}_${client.id.toString().take(8)}"
            client.domain = null
            registeredClientRepository.save(client)
        }
        log.debug { "Soft-deleted ${clients.size} registered clients for organization: $organizationId" }
    }

    private fun anonymizeAndMarkDeleted(organization: Organization) {
        val anonymizedSuffix = organization.id.toString().take(8)

        organization.name = "${ANONYMIZED_ORG_PREFIX}_$anonymizedSuffix"
        organization.slug = "${ANONYMIZED_ORG_PREFIX}_$anonymizedSuffix"
        organization.status = OrganizationStatus.DELETED
        organization.settings = emptyMap()
        organization.updatedAt = Instant.now()

        organizationRepository.save(organization)
        log.debug { "Anonymized and marked organization as deleted: ${organization.id}" }
    }

    companion object {
        private const val ANONYMIZED_ORG_PREFIX = "deleted_org"
        private const val ANONYMIZED_CLIENT_PREFIX = "deleted_client"
    }
}

data class OrganizationDeletionResult(
    val success: Boolean,
    val message: String,
    val organizationId: UUID,
    val usersDeleted: Int
)
