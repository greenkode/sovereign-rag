package ai.sovereignrag.identity.core.organization.controller

import ai.sovereignrag.commons.internal.OrganizationResponse
import ai.sovereignrag.commons.internal.UpdateOrganizationDatabaseRequest
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.identity.core.organization.repository.OrganizationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/organizations")
class OrganizationInternalController(
    private val organizationRepository: OrganizationRepository
) {

    @GetMapping("/{organizationId}")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    fun getOrganization(@PathVariable organizationId: UUID): OrganizationResponse {
        log.info { "Fetching organization: $organizationId" }

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { RecordNotFoundException("Organization not found: $organizationId") }

        return OrganizationResponse(
            id = organization.id,
            name = organization.name,
            slug = organization.slug,
            databaseName = organization.databaseName,
            databaseCreated = organization.databaseCreated,
            maxKnowledgeBases = organization.maxKnowledgeBases
        )
    }

    @PatchMapping("/{organizationId}/database")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun updateOrganizationDatabase(
        @PathVariable organizationId: UUID,
        @RequestBody request: UpdateOrganizationDatabaseRequest
    ): OrganizationResponse {
        log.info { "Updating database for organization: $organizationId -> ${request.databaseName}" }

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { RecordNotFoundException("Organization not found: $organizationId") }

        organization.databaseName = request.databaseName
        organization.databaseCreated = true
        organization.updatedAt = Instant.now()

        organizationRepository.save(organization)

        log.info { "Organization database updated successfully: $organizationId" }

        return OrganizationResponse(
            id = organization.id,
            name = organization.name,
            slug = organization.slug,
            databaseName = organization.databaseName,
            databaseCreated = organization.databaseCreated,
            maxKnowledgeBases = organization.maxKnowledgeBases
        )
    }
}
