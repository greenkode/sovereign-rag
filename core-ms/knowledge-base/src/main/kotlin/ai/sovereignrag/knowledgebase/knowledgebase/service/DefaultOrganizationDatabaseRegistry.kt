package ai.sovereignrag.knowledgebase.knowledgebase.service

import ai.sovereignrag.knowledgebase.knowledgebase.gateway.IdentityServiceGateway
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Service
class DefaultOrganizationDatabaseRegistry(
    private val identityServiceGateway: IdentityServiceGateway
) : OrganizationDatabaseRegistry {

    private val cache = ConcurrentHashMap<UUID, CachedOrganizationInfo>()

    override fun getDatabaseName(organizationId: UUID): String {
        return getOrFetchOrganization(organizationId).let { info ->
            info.databaseName ?: generateDatabaseName(organizationId, info.slug)
        }
    }

    override fun isDatabaseCreated(organizationId: UUID): Boolean {
        return getOrFetchOrganization(organizationId).databaseCreated
    }

    override fun markDatabaseCreated(organizationId: UUID, databaseName: String) {
        identityServiceGateway.updateOrganizationDatabase(organizationId, databaseName)
        cache[organizationId] = cache[organizationId]?.copy(
            databaseName = databaseName,
            databaseCreated = true
        ) ?: CachedOrganizationInfo(
            slug = "",
            databaseName = databaseName,
            databaseCreated = true
        )
        log.info { "Marked database as created for organization: $organizationId" }
    }

    private fun getOrFetchOrganization(organizationId: UUID): CachedOrganizationInfo {
        return cache.getOrPut(organizationId) {
            val info = identityServiceGateway.getOrganization(organizationId)
            CachedOrganizationInfo(
                slug = info.slug,
                databaseName = info.databaseName,
                databaseCreated = info.databaseCreated
            )
        }
    }

    private fun generateDatabaseName(organizationId: UUID, slug: String): String {
        val uuidWithoutDashes = organizationId.toString().replace("-", "")
        return "sovereignrag_$uuidWithoutDashes"
    }

    private data class CachedOrganizationInfo(
        val slug: String,
        val databaseName: String?,
        val databaseCreated: Boolean
    )
}
