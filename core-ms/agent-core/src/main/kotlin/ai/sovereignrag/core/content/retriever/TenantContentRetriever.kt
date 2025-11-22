package ai.sovereignrag.core.content.retriever

import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.query.Query
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import mu.KotlinLogging
import nl.compilot.ai.content.store.TenantAwarePgVectorStoreFactory
import nl.compilot.ai.commons.tenant.TenantContext
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Content retriever that searches tenant-specific document segments
 * Uses LangChain4j's PgVectorEmbeddingStore
 *
 * This implements LangChain4j's ContentRetriever interface for RAG pipelines
 */
@Component
class TenantContentRetriever(
    private val embeddingModel: EmbeddingModel,
    private val pgVectorStoreFactory: TenantAwarePgVectorStoreFactory,
    private val queryClassifier: nl.compilot.ai.content.service.QueryClassifier
) : ContentRetriever {

    /**
     * Retrieve relevant content for a query
     *
     * @param query User's query
     * @return List of relevant content chunks
     */
    override fun retrieve(query: Query): List<Content> {
        val tenantId = TenantContext.getCurrentTenant()
        val queryText = query.text()

        // Classify query type
        val queryType = queryClassifier.classifyQuery(queryText)

        logger.debug { "[$tenantId] Retrieving content for $queryType query: $queryText" }

        return when (queryType) {
            nl.compilot.ai.content.service.QueryType.CONTEXTUAL -> retrieveContextualContent(queryText)
            nl.compilot.ai.content.service.QueryType.SPECIFIC -> retrieveSpecificContent(queryText)
        }
    }

    /**
     * Retrieve content for high-level contextual queries
     * Prioritizes site profiles, category summaries, and FAQs
     */
    private fun retrieveContextualContent(queryText: String): List<Content> {
        val tenantId = TenantContext.getCurrentTenant()
        val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()
        val queryEmbedding = embeddingModel.embed(queryText).content()

        logger.info { "[$tenantId] Using contextual retrieval strategy" }

        // Search for contextual documents (site profiles, summaries, FAQs)
        val contextualRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(10)
            .minScore(0.5)
            .build()

        val allResults = embeddingStore.search(contextualRequest)

        // Prioritize contextual document types
        val contextualDocs = allResults.matches().filter { match ->
            val docType = match.embedded()?.metadata()?.get("post_type")?.toString()
            docType in listOf("site_profile", "category_summary", "synthetic_faq")
        }.take(3)

        // Add some specific content for details
        val specificDocs = allResults.matches().filter { match ->
            val docType = match.embedded()?.metadata()?.get("post_type")?.toString()
            docType !in listOf("site_profile", "category_summary", "synthetic_faq")
        }.take(2)

        val combinedResults = (contextualDocs + specificDocs).distinctBy { it.embeddingId() }

        logger.info { "[$tenantId] Contextual retrieval: ${contextualDocs.size} contextual + ${specificDocs.size} specific docs" }

        return combinedResults.map { match ->
            Content.from(match.embedded()?.text() ?: "")
        }
    }

    /**
     * Standard retrieval for specific queries
     */
    private fun retrieveSpecificContent(queryText: String): List<Content> {
        val tenantId = TenantContext.getCurrentTenant()
        val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()
        val queryEmbedding = embeddingModel.embed(queryText).content()

        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(5)
            .minScore(0.7)
            .build()

        val searchResult = embeddingStore.search(searchRequest)

        logger.info { "[$tenantId] Found ${searchResult.matches().size} relevant segments (specific)" }

        return searchResult.matches().map { match ->
            Content.from(match.embedded()?.text() ?: "")
        }
    }
}
