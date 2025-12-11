package ai.sovereignrag.core.llm

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmPrivacyLevel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType
import ai.sovereignrag.knowledgebase.configuration.repository.LlmModelRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class LlmModelSelectionService(
    private val llmModelRepository: LlmModelRepository
) {

    fun getModelById(modelId: String): LlmModel? =
        llmModelRepository.findById(modelId).orElse(null)

    fun getDefaultModel(): LlmModel =
        llmModelRepository.findByIsDefaultTrueAndEnabledTrue()
            ?: llmModelRepository.findLocalModels().firstOrNull()
            ?: throw IllegalStateException("No default LLM model configured")

    fun getAvailableModels(tier: SubscriptionTier): List<LlmModel> {
        log.debug { "Getting available LLM models for tier: $tier" }
        return llmModelRepository.findAccessibleByTier(tier.priority)
    }

    fun getPrivacyCompliantModels(tier: SubscriptionTier): List<LlmModel> {
        log.debug { "Getting privacy-compliant (local) LLM models for tier: $tier" }
        return llmModelRepository.findByPrivacyLevelAndTier(LlmPrivacyLevel.MAXIMUM, tier.priority)
    }

    fun getCloudModels(tier: SubscriptionTier): List<LlmModel> {
        log.debug { "Getting cloud LLM models for tier: $tier" }
        return llmModelRepository.findCloudModelsAccessibleByTier(tier.priority)
    }

    fun getModelsByCapability(capabilities: Set<String>, tier: SubscriptionTier): List<LlmModel> {
        log.debug { "Getting LLM models with capabilities $capabilities for tier: $tier" }
        return llmModelRepository.findByCapabilitiesAndTier(capabilities, tier.priority)
    }

    fun validateModelAccess(modelId: String, tier: SubscriptionTier): ModelAccessResult {
        val model = llmModelRepository.findById(modelId).orElse(null)
            ?: return ModelAccessResult.NotFound

        if (!model.enabled) {
            return ModelAccessResult.Disabled
        }

        if (!model.isAccessibleByTier(tier)) {
            return ModelAccessResult.TierRestricted(model.minTier)
        }

        return ModelAccessResult.Allowed(model)
    }

    fun resolveModelForKnowledgeBase(
        llmModelId: String?,
        tier: SubscriptionTier,
        requiresPrivacy: Boolean = false
    ): LlmModel {
        llmModelId?.let { modelId ->
            val accessResult = validateModelAccess(modelId, tier)
            return when (accessResult) {
                is ModelAccessResult.Allowed -> accessResult.model
                is ModelAccessResult.TierRestricted -> {
                    log.warn { "Model $modelId requires tier ${accessResult.requiredTier}, user has $tier. Falling back to default." }
                    getDefaultModelForTier(tier, requiresPrivacy)
                }
                ModelAccessResult.Disabled -> {
                    log.warn { "Model $modelId is disabled. Falling back to default." }
                    getDefaultModelForTier(tier, requiresPrivacy)
                }
                ModelAccessResult.NotFound -> {
                    log.warn { "Model $modelId not found. Falling back to default." }
                    getDefaultModelForTier(tier, requiresPrivacy)
                }
            }
        }
        return getDefaultModelForTier(tier, requiresPrivacy)
    }

    private fun getDefaultModelForTier(tier: SubscriptionTier, requiresPrivacy: Boolean): LlmModel {
        if (requiresPrivacy) {
            return getPrivacyCompliantModels(tier).firstOrNull()
                ?: getDefaultModel()
        }
        val defaultModel = llmModelRepository.findByIsDefaultTrueAndEnabledTrue()
        return defaultModel?.takeIf { it.isAccessibleByTier(tier) }
            ?: getAvailableModels(tier).firstOrNull()
            ?: getDefaultModel()
    }

    fun getModelsByProvider(provider: String): List<LlmModel> =
        llmModelRepository.findByEnabledTrueOrderBySortOrder()
            .filter { it.provider.equals(provider, ignoreCase = true) }

    fun getLocalModels(): List<LlmModel> =
        llmModelRepository.findByProviderTypeAndEnabledTrueOrderBySortOrder(LlmProviderType.LOCAL)

    fun getAllEnabledModels(): List<LlmModel> =
        llmModelRepository.findByEnabledTrueOrderBySortOrder()
}

sealed class ModelAccessResult {
    data class Allowed(val model: LlmModel) : ModelAccessResult()
    data class TierRestricted(val requiredTier: SubscriptionTier) : ModelAccessResult()
    data object Disabled : ModelAccessResult()
    data object NotFound : ModelAccessResult()
}
