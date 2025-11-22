package nl.compilot.ai.commons.prompt

/**
 * Interface for rendering prompt templates with parameter substitution
 *
 * Implementations handle runtime substitution of ${parameter} placeholders
 * with actual values from a parameter map.
 */
interface PromptTemplateRenderer {

    /**
     * Render a template string with parameter substitution
     *
     * @param template The template text with ${parameter} placeholders
     * @param parameters Map of parameter names to values for substitution
     * @return The rendered template with all parameters substituted
     * @throws TemplateRenderException if rendering fails or required parameters are missing
     */
    fun render(template: String, parameters: Map<String, Any>): String

    /**
     * Render a template string with parameter substitution (vararg convenience)
     *
     * @param template The template text with ${parameter} placeholders
     * @param parameters Pairs of parameter names to values
     * @return The rendered template with all parameters substituted
     */
    fun render(template: String, vararg parameters: Pair<String, Any>): String {
        return render(template, parameters.toMap())
    }

    /**
     * Validate that a template has all required parameters
     *
     * @param template The template text to validate
     * @param parameters Map of available parameter names
     * @return List of missing parameter names (empty if all present)
     */
    fun validateParameters(template: String, parameters: Set<String>): List<String>
}

/**
 * Exception thrown when template rendering fails
 */
class TemplateRenderException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
