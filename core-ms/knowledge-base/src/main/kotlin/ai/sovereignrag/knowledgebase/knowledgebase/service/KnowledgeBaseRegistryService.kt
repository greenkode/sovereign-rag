package ai.sovereignrag.knowledgebase.knowledgebase.service

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseInfo
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.knowledgebase.knowledgebase.domain.KnowledgeBase
import ai.sovereignrag.knowledgebase.knowledgebase.repository.KnowledgeBaseRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional(transactionManager = "masterTransactionManager")
class KnowledgeBaseRegistryService(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder
) : KnowledgeBaseRegistry {

    @Cacheable(cacheNames = ["knowledge_bases"], key = "#knowledgeBaseId")
    override fun getKnowledgeBase(knowledgeBaseId: String): KnowledgeBaseInfo {
        return knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId)
            ?: throw KnowledgeBaseNotFoundException("Knowledge base not found: $knowledgeBaseId")
    }

    override fun getKnowledgeBasesByOrganization(organizationId: UUID): List<KnowledgeBaseInfo> {
        return knowledgeBaseRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
    }

    override fun updateLastActive(knowledgeBaseId: String) {
        knowledgeBaseRepository.updateLastActive(knowledgeBaseId, Instant.now())
    }

    override fun validateApiKey(knowledgeBaseId: String, apiKey: String): KnowledgeBaseInfo? {
        val kb = knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId) ?: return null
        val hash = kb.apiKeyHash ?: return null
        return kb.takeIf { passwordEncoder.matches(apiKey, hash) }
    }

    @CacheEvict(cacheNames = ["knowledge_bases"], allEntries = true)
    fun createKnowledgeBase(
        id: String,
        name: String,
        organizationId: UUID,
        schemaName: String,
        regionCode: String,
        description: String? = null,
        embeddingModelId: String? = null,
        llmModelId: String? = null,
        requiresEncryption: Boolean? = null,
        contactEmail: String? = null,
        contactName: String? = null
    ): KnowledgeBase {
        log.info { "Creating knowledge base: $id ($name) for organization: $organizationId in region: $regionCode" }

        val knowledgeBase = KnowledgeBase(
            id = id,
            name = name,
            description = description,
            organizationId = organizationId,
            schemaName = schemaName,
            regionCode = regionCode,
            status = KnowledgeBaseStatus.ACTIVE,
            embeddingModelId = embeddingModelId,
            llmModelId = llmModelId,
            requiresEncryption = requiresEncryption ?: false,
            contactEmail = contactEmail,
            contactName = contactName
        )

        return knowledgeBaseRepository.save(knowledgeBase).also {
            log.info { "Knowledge base created: ${it.id} in region: $regionCode" }
        }
    }

    @CacheEvict(cacheNames = ["knowledge_bases"], key = "#knowledgeBaseId")
    fun updateOauthClientId(knowledgeBaseId: String, oauthClientId: String) {
        knowledgeBaseRepository.updateOauthClientId(knowledgeBaseId, oauthClientId, Instant.now())
        log.info { "Updated OAuth client ID for knowledge base: $knowledgeBaseId" }
    }

    fun listKnowledgeBases(status: KnowledgeBaseStatus? = null): List<KnowledgeBase> {
        return status?.let {
            knowledgeBaseRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(it)
        } ?: knowledgeBaseRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
    }

    fun listKnowledgeBasesByOrganization(
        organizationId: UUID,
        status: KnowledgeBaseStatus? = null
    ): List<KnowledgeBase> {
        return status?.let {
            knowledgeBaseRepository.findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId, it)
        } ?: knowledgeBaseRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
    }

    fun countByOrganization(organizationId: UUID): Long {
        return knowledgeBaseRepository.countByOrganizationIdAndDeletedAtIsNull(organizationId)
    }

    fun findByOauthClientId(oauthClientId: String): KnowledgeBase? {
        return knowledgeBaseRepository.findByOauthClientIdAndDeletedAtIsNull(oauthClientId)
    }

    @CacheEvict(cacheNames = ["knowledge_bases"], key = "#knowledgeBaseId")
    fun deleteKnowledgeBase(knowledgeBaseId: String, hardDelete: Boolean = false) {
        val knowledgeBase = knowledgeBaseRepository.findByIdAndDeletedAtIsNull(knowledgeBaseId)
            ?: throw KnowledgeBaseNotFoundException("Knowledge base not found: $knowledgeBaseId")

        if (hardDelete) {
            log.warn { "HARD DELETE knowledge base: $knowledgeBaseId" }
            knowledgeBaseRepository.deleteById(knowledgeBaseId)
        } else {
            log.info { "Soft delete knowledge base: $knowledgeBaseId" }
            val now = Instant.now()
            knowledgeBaseRepository.softDelete(knowledgeBaseId, now, now)
        }
    }
}
