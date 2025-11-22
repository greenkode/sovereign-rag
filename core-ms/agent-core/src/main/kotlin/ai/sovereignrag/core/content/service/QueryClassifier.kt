package ai.sovereignrag.core.content.service

import org.springframework.stereotype.Service

/**
 * Classifies user queries to determine retrieval strategy
 */
@Service
class QueryClassifier {

    private val highLevelPatterns = listOf(
        // About the site/service
        "what.*this.*about",
        "what.*website.*about",
        "what.*site.*about",
        "tell me about",
        "tell.*about (you|this)",

        // Services/products
        "what.*you.*do",
        "what.*you.*offer",
        "what.*services",
        "what.*products",
        "what.*provide",

        // General overview
        "who are you",
        "who.*company",
        "overview",
        "introduction",

        // Help/support
        "how.*help",
        "what.*help with",
        "how.*contact",
        "how.*reach",

        // Pricing/availability (but still contextual)
        "do you.*offer",
        "do you.*have",
        "can you.*help",

        // Mission/purpose
        "what.*mission",
        "what.*goal",
        "why.*exist"
    )

    fun isHighLevelQuery(query: String): Boolean {
        val normalized = query.lowercase().trim()

        return highLevelPatterns.any { pattern ->
            normalized.matches(Regex(pattern))
        }
    }

    fun classifyQuery(query: String): QueryType {
        return when {
            isHighLevelQuery(query) -> QueryType.CONTEXTUAL
            else -> QueryType.SPECIFIC
        }
    }
}

enum class QueryType {
    CONTEXTUAL,  // About site, services, general info - needs site profile/summaries
    SPECIFIC     // Specific question about content - needs standard retrieval
}
