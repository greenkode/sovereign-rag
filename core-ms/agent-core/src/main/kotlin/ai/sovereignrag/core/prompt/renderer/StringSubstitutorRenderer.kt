package ai.sovereignrag.core.prompt.renderer

import mu.KotlinLogging
import nl.compilot.ai.commons.prompt.PromptTemplateRenderer
import nl.compilot.ai.commons.prompt.TemplateRenderException
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Template renderer using Apache Commons Text StringSubstitutor
 *
 * Supports ${parameter} syntax for parameter substitution.
 * Supports nested properties like ${user.email}.
 * Missing parameters are left as-is (e.g., ${missing} remains unchanged).
 */
@Component
class StringSubstitutorRenderer : PromptTemplateRenderer {

    companion object {
        private const val PREFIX = "\${"
        private const val SUFFIX = "}"
        private val PARAMETER_REGEX = Regex("""\$\{([^}]+)}""")
    }

    override fun render(template: String, parameters: Map<String, Any>): String {
        return try {
            val substitutor = StringSubstitutor(parameters, PREFIX, SUFFIX)

            // Don't throw on missing parameters - leave them as ${param} for visibility
            substitutor.isEnableUndefinedVariableException = false

            val result = substitutor.replace(template)

            // Log warning if any parameters weren't substituted
            val missingParams = PARAMETER_REGEX.findAll(result)
                .map { it.groupValues[1] }
                .filter { !parameters.containsKey(it) }
                .toList()

            if (missingParams.isNotEmpty()) {
                logger.warn {
                    "Template rendered with missing parameters: ${missingParams.joinToString(", ")}. " +
                    "These will appear as \${param} in output."
                }
            }

            result
        } catch (e: Exception) {
            throw TemplateRenderException(
                "Failed to render template: ${e.message}",
                e
            )
        }
    }

    override fun validateParameters(template: String, parameters: Set<String>): List<String> {
        val requiredParams = PARAMETER_REGEX.findAll(template)
            .map { it.groupValues[1] }
            .toSet()

        return requiredParams.filterNot { parameters.contains(it) }
    }
}
