package ai.sovereignrag.core.prompt.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import nl.compilot.ai.commons.prompt.PromptTemplateRenderer
import nl.compilot.ai.prompt.domain.PersonaConfiguration
import nl.compilot.ai.prompt.repository.PersonaConfigurationRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Service for loading and assembling persona configurations
 *
 * Handles:
 * - Tenant-specific persona overrides
 * - Assembling personas from multiple templates (base + language + escalation)
 * - Parameter merging and rendering
 * - Persona caching
 */
@Service
@Transactional(readOnly = true)
class PersonaService(
    private val personaRepository: PersonaConfigurationRepository,
    private val templateService: PromptTemplateService,
    private val renderer: PromptTemplateRenderer,
    private val objectMapper: ObjectMapper
) {

    /**
     * Get persona configuration with tenant override support
     *
     * @param personaKey Persona identifier (customer_service, professional, etc.)
     * @param tenantId Tenant ID (null for global)
     * @return PersonaConfiguration or null if not found
     */
    @Cacheable(value = ["personaConfigurations"], key = "#personaKey + ':' + #tenantId")
    fun getPersonaConfiguration(
        personaKey: String,
        tenantId: String?
    ): PersonaConfiguration? {
        val personas = personaRepository.findByPersonaKeyWithTenantOverride(
            personaKey = personaKey,
            tenantId = tenantId
        )

        return personas.firstOrNull().also { persona ->
            if (persona == null) {
                logger.warn { "Persona not found: personaKey=$personaKey, tenantId=$tenantId" }
            } else {
                logger.debug {
                    "Loaded persona: personaKey=$personaKey, " +
                    "tenantId=${persona.tenantId ?: "global"}, displayName=${persona.displayName}"
                }
            }
        }
    }

    /**
     * Assemble complete persona prompt from a PersonaConfiguration object
     *
     * Combines base template + optional language template + optional escalation template
     * and renders all with merged parameters.
     *
     * This method expects the caller to fetch the PersonaConfiguration first using
     * getPersonaConfiguration() (which is cached), and then pass it here for assembly.
     * This design ensures proper cache utilization and separation of concerns.
     *
     * @param persona PersonaConfiguration object (fetch via getPersonaConfiguration first)
     * @param runtimeParameters Additional parameters for runtime substitution
     * @return Assembled and rendered persona prompt
     */
    fun assemblePersonaPrompt(
        persona: PersonaConfiguration,
        runtimeParameters: Map<String, Any> = emptyMap()
    ): String {
        // Load all templates for this persona
        val templateIds = listOfNotNull(
            persona.baseTemplateId,
            persona.languageTemplateId,
            persona.escalationTemplateId
        )

        val templates = templateService.getTemplatesByIds(templateIds)

        // Get base template (required)
        val baseTemplate = templates[persona.baseTemplateId]
            ?: throw IllegalStateException("Base template not found: ${persona.baseTemplateId}")

        // Merge default parameters from persona with runtime parameters
        val defaultParams = persona.parameters?.let { json ->
            try {
                objectMapper.readValue<Map<String, Any>>(json)
            } catch (e: Exception) {
                logger.warn { "Failed to parse persona parameters JSON: ${e.message}" }
                emptyMap()
            }
        } ?: emptyMap()

        val mergedParams = defaultParams + runtimeParameters

        // Render base template
        val renderedBase = renderer.render(baseTemplate.templateText, mergedParams)

        // Append language template if present
        val withLanguage = persona.languageTemplateId?.let { templateId ->
            templates[templateId]?.let { languageTemplate ->
                val renderedLanguage = renderer.render(languageTemplate.templateText, mergedParams)
                "$renderedBase\n\n$renderedLanguage"
            } ?: renderedBase
        } ?: renderedBase

        // Append escalation template if present
        val complete = persona.escalationTemplateId?.let { templateId ->
            templates[templateId]?.let { escalationTemplate ->
                val renderedEscalation = renderer.render(escalationTemplate.templateText, mergedParams)
                "$withLanguage\n\n$renderedEscalation"
            } ?: withLanguage
        } ?: withLanguage

        logger.debug {
            "Assembled persona prompt: personaKey=${persona.personaKey}, " +
            "templates=[base:${baseTemplate.id}, " +
            "language:${persona.languageTemplateId}, " +
            "escalation:${persona.escalationTemplateId}], " +
            "params=${mergedParams.keys.joinToString()}"
        }

        return complete
    }

    /**
     * Get all available personas for a tenant
     *
     * @param tenantId Tenant ID (null for global only)
     * @return List of active persona configurations
     */
    @Cacheable(value = ["personaList"], key = "#tenantId ?: 'global'")
    fun getAvailablePersonas(tenantId: String?): List<PersonaConfiguration> {
        return personaRepository.findActivePersonasForTenant(tenantId)
    }

    /**
     * Data class for persona summary (lighter than full config)
     */
    data class PersonaSummary(
        val personaKey: String,
        val displayName: String,
        val description: String?,
        val isCustom: Boolean  // true if tenant-specific
    )

    /**
     * Get lightweight persona summaries for UI display
     *
     * @param tenantId Tenant ID
     * @return List of persona summaries
     */
    fun getPersonaSummaries(tenantId: String?): List<PersonaSummary> {
        return getAvailablePersonas(tenantId).map { persona ->
            PersonaSummary(
                personaKey = persona.personaKey,
                displayName = persona.displayName,
                description = persona.description,
                isCustom = persona.tenantId != null
            )
        }
    }
}
