package ai.sovereignrag.core.content.event

import mu.KotlinLogging
import dev.langchain4j.model.embedding.EmbeddingModel
import nl.compilot.ai.content.service.ContextAggregationService
import nl.compilot.ai.content.store.TenantAwarePgVectorStoreFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Listens for content ingestion events and triggers contextual enrichment
 * Processes events asynchronously to avoid blocking content ingestion
 * SecurityContext is automatically propagated via @Async (see AsyncSecurityConfig)
 */
@Component
class ContextEnrichmentListener(
    private val contextAggregationService: ContextAggregationService,
    private val embeddingModel: EmbeddingModel,
    private val pgVectorStoreFactory: TenantAwarePgVectorStoreFactory
) {

    @EventListener
    @Async
    fun onContentIngestion(event: ContentIngestionEvent) {
        logger.info { "[${event.tenantId}] Content ingestion event received for: ${event.title}" }

        try {
            // 1. Update category summary if category exists
            event.category?.let { category ->
                logger.info { "[${event.tenantId}] Updating category summary: $category" }
                contextAggregationService.generateCategorySummary(category)
            }

            // 2. Incrementally update site profile on EVERY ingestion
            logger.info { "[${event.tenantId}] Incrementally updating site profile" }
            contextAggregationService.incrementallyUpdateSiteProfile(event)

            // 3. Periodically regenerate FAQs (these are less critical)
            val documentCount = getDocumentCount(event.tenantId)
            if (shouldGenerateFAQs(documentCount)) {
                logger.info { "[${event.tenantId}] Generating FAQs (doc count: $documentCount)" }
                contextAggregationService.generateCommonQuestions()
            }

            logger.info { "[${event.tenantId}] Context enrichment completed for: ${event.title}" }

        } catch (e: Exception) {
            logger.error(e) { "[${event.tenantId}] Context enrichment failed for: ${event.title}" }
        }
    }

    private fun getDocumentCount(tenantId: String): Int {
        return try {
            val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()
            val dummyEmbedding = embeddingModel.embed("count").content()

            val searchRequest = dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .maxResults(10000)
                .minScore(0.0)
                .build()

            val results = embeddingStore.search(searchRequest)

            // Count unique documents (not chunks)
            results.matches()
                .mapNotNull { it.embedded()?.metadata()?.get("documentId")?.toString() }
                .distinct()
                .size
        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to get document count" }
            0
        }
    }

    private fun shouldRegenerateSiteProfile(count: Int): Boolean {
        // Regenerate on first doc, then every 25 docs, or if count is exactly 10 (good initial profile)
        return count == 1 || count == 10 || count % 25 == 0
    }

    private fun shouldGenerateFAQs(count: Int): Boolean {
        // Generate FAQs starting at 5 docs, then every 20 docs
        return count == 5 || (count >= 20 && count % 20 == 0)
    }
}
