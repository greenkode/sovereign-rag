package ai.sovereignrag.knowledgebase.configuration.repository

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmPrivacyLevel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LlmModelRepository : JpaRepository<LlmModel, String> {

    fun findByEnabledTrueOrderBySortOrder(): List<LlmModel>

    fun findByIsDefaultTrueAndEnabledTrue(): LlmModel?

    fun findByProviderTypeAndEnabledTrueOrderBySortOrder(providerType: LlmProviderType): List<LlmModel>

    fun findByPrivacyLevelAndEnabledTrueOrderBySortOrder(privacyLevel: LlmPrivacyLevel): List<LlmModel>

    @Query("""
        SELECT m FROM LlmModel m
        WHERE m.enabled = true
        AND m.minTier.priority <= :tierPriority
        ORDER BY m.sortOrder
    """)
    fun findAccessibleByTier(tierPriority: Int): List<LlmModel>

    @Query("""
        SELECT DISTINCT m FROM LlmModel m
        JOIN m.capabilities c
        WHERE c IN :capabilities AND m.enabled = true
        ORDER BY m.sortOrder
    """)
    fun findByCapabilities(capabilities: Set<String>): List<LlmModel>

    @Query("""
        SELECT DISTINCT m FROM LlmModel m
        JOIN m.capabilities c
        WHERE c IN :capabilities
        AND m.enabled = true
        AND m.minTier.priority <= :tierPriority
        ORDER BY m.sortOrder
    """)
    fun findByCapabilitiesAndTier(capabilities: Set<String>, tierPriority: Int): List<LlmModel>

    @Query("""
        SELECT m FROM LlmModel m
        WHERE m.enabled = true
        AND m.privacyLevel = :privacyLevel
        AND m.minTier.priority <= :tierPriority
        ORDER BY m.sortOrder
    """)
    fun findByPrivacyLevelAndTier(privacyLevel: LlmPrivacyLevel, tierPriority: Int): List<LlmModel>

    @Query("""
        SELECT m FROM LlmModel m
        WHERE m.enabled = true
        AND m.providerType = ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType.LOCAL
        ORDER BY m.sortOrder
    """)
    fun findLocalModels(): List<LlmModel>

    @Query("""
        SELECT m FROM LlmModel m
        WHERE m.enabled = true
        AND m.providerType = ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType.CLOUD
        AND m.minTier.priority <= :tierPriority
        ORDER BY m.sortOrder
    """)
    fun findCloudModelsAccessibleByTier(tierPriority: Int): List<LlmModel>
}
