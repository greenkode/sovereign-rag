package ai.sovereignrag.core.prompt.service

import mu.KotlinLogging
import ai.sovereignrag.commons.prompt.PromptTemplateRenderer
import ai.sovereignrag.prompt.domain.PromptTemplate
import ai.sovereignrag.prompt.repository.PromptTemplateRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Service for loading and rendering prompt templates
 *
 * Handles tenant-specific overrides and template caching for performance.
 */
@Service
@Transactional(readOnly = true)
class PromptTemplateService(
    private val templateRepository: PromptTemplateRepository,
    private val renderer: PromptTemplateRenderer
) {

    /**
     * Get template by category and name with tenant override support
     *
     * Returns tenant-specific template if exists, otherwise global template.
     * Results are cached by (category, name, tenantId) key.
     *
     * @param category Template category (persona, system, instruction)
     * @param name Template name
     * @param tenantId Tenant ID (null for global lookup)
     * @return PromptTemplate or null if not found
     */
    @Cacheable(value = ["promptTemplates"], key = "#category + ':' + #name + ':' + #tenantId")
    fun getTemplate(
        category: String,
        name: String,
        tenantId: String?
    ): PromptTemplate? {
        val templates = templateRepository.findByCategoryAndNameWithTenantOverride(
            category = category,
            name = name,
            tenantId = tenantId
        )

        return templates.firstOrNull().also { template ->
            if (template == null) {
                logger.warn { "Template not found: category=$category, name=$name, tenantId=$tenantId" }
            } else {
                logger.debug {
                    "Loaded template: category=$category, name=$name, " +
                    "tenantId=${template.tenantId ?: "global"}, version=${template.version}"
                }
            }
        }
    }

    /**
     * Render template with parameter substitution
     *
     * This method expects the caller to fetch the PromptTemplate first using
     * getTemplate() (which is cached), and then pass it here for rendering.
     * This design ensures proper cache utilization and separation of concerns.
     *
     * @param template PromptTemplate object (fetch via getTemplate first)
     * @param parameters Map of parameter names to values
     * @return Rendered template text
     */
    fun renderTemplate(
        template: PromptTemplate,
        parameters: Map<String, Any>
    ): String {
        return renderer.render(template.templateText, parameters)
    }

    /**
     * Render template with parameter substitution (vararg convenience)
     *
     * @param template PromptTemplate object (fetch via getTemplate first)
     * @param parameters Variable number of parameter pairs
     * @return Rendered template text
     */
    fun renderTemplate(
        template: PromptTemplate,
        vararg parameters: Pair<String, Any>
    ): String {
        return renderTemplate(template, parameters.toMap())
    }

    /**
     * Get all templates for a specific category and tenant
     *
     * @param category Template category
     * @param tenantId Tenant ID (null for global only)
     * @return List of active templates for the category
     */
    @Cacheable(value = ["promptTemplatesByCategory"], key = "#category + ':' + #tenantId")
    fun getTemplatesByCategory(
        category: String,
        tenantId: String?
    ): List<PromptTemplate> {
        return if (tenantId != null) {
            templateRepository.findActiveTemplatesForTenant(tenantId)
                .filter { it.category == category }
        } else {
            templateRepository.findByCategoryAndActiveTrueOrderByNameAsc(category)
        }
    }

    /**
     * Get templates by ID list (for persona template lookups)
     *
     * @param ids List of template IDs
     * @return Map of template ID to PromptTemplate
     */
    fun getTemplatesByIds(ids: List<Long>): Map<Long, PromptTemplate> {
        return templateRepository.findByIdIn(ids)
            .associateBy { it.id!! }
    }

    /**
     * Get single template by ID
     *
     * @param id Template ID
     * @return PromptTemplate or null if not found
     */
    @Cacheable(value = ["promptTemplatesById"], key = "#id")
    fun getTemplateById(id: Long): PromptTemplate? {
        return templateRepository.findById(id).orElse(null)
    }
}
