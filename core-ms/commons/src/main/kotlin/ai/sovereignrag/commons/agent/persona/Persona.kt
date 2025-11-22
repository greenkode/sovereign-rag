package ai.sovereignrag.commons.agent.persona

/**
 * Persona defines the agent's behavior, personality, and capabilities.
 *
 * A persona includes:
 * - Identity (name, role)
 * - System prompt (instructions for the LLM)
 * - Personality traits
 * - Capabilities (tools, knowledge domains)
 * - Response guidelines
 *
 * Personas allow you to customize the same base agent for different use cases:
 * - Customer service agent
 * - Technical support agent
 * - Sales agent
 * - General assistant
 */
data class Persona(
    /**
     * Unique identifier for this persona
     */
    val id: String,

    /**
     * Human-readable name
     */
    val name: String,

    /**
     * Role or title (e.g., "Customer Service Agent", "Technical Support")
     */
    val role: String,

    /**
     * System prompt defining the agent's behavior and instructions.
     * This is the primary way to control the agent's personality and responses.
     */
    val systemPrompt: String,

    /**
     * Personality traits (e.g., "friendly", "professional", "concise")
     */
    val traits: List<String> = emptyList(),

    /**
     * Available tool names for this persona
     */
    val availableTools: List<String> = emptyList(),

    /**
     * Knowledge domains this persona specializes in
     */
    val knowledgeDomains: List<String> = emptyList(),

    /**
     * Response guidelines (e.g., "Always provide sources", "Be concise")
     */
    val responseGuidelines: List<String> = emptyList(),

    /**
     * Tone/style of responses
     */
    val tone: Tone = Tone.PROFESSIONAL,

    /**
     * Additional configuration
     */
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Tone/style options
     */
    enum class Tone {
        PROFESSIONAL,
        FRIENDLY,
        CASUAL,
        FORMAL,
        EMPATHETIC,
        CONCISE,
        DETAILED
    }
}
