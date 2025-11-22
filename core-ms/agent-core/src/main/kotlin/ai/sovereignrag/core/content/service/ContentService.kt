package ai.sovereignrag.core.content.service

import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import mu.KotlinLogging
import ai.sovereignrag.content.store.TenantAwarePgVectorStoreFactory
import ai.sovereignrag.domain.ContentDocument
import ai.sovereignrag.domain.SearchResult
import ai.sovereignrag.service.RerankerService
import ai.sovereignrag.commons.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Content service using LangChain4j's PgVectorEmbeddingStore
 * All document storage, chunking, and embedding management is handled by LangChain4j
 */
@Service
@Transactional
class ContentService(
    private val embeddingModel: EmbeddingModel,
    private val rerankerService: RerankerService,
    private val documentChunkingService: DocumentChunkingService,
    private val pgVectorStoreFactory: TenantAwarePgVectorStoreFactory,
    private val sovereignragProperties: ai.sovereignrag.config.SovereignRagProperties
) {

    /**
     * Ingest content document using LangChain4j's PgVectorEmbeddingStore
     * Everything (chunking, embeddings, storage) is managed by LangChain4j
     */
    fun ingest(contentDoc: ContentDocument) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Ingesting document: ${contentDoc.title} (${contentDoc.url})" }

        try {
            val documentId = UUID.randomUUID()

            // Chunk document and store with embeddings in LangChain4j's PgVectorEmbeddingStore
            // Pass all metadata including enriched contextual info
            val enrichedMetadata = buildMap<String, Any> {
                put("url", contentDoc.url ?: "")
                put("source", contentDoc.source ?: "")
                put("documentType", contentDoc.metadata["post_type"] ?: "page")
                put("language", contentDoc.metadata["language"] ?: "en")
                put("documentId", documentId.toString())
                put("title", contentDoc.title)

                // Pass through all other metadata
                contentDoc.metadata.forEach { (key, value) ->
                    if (key !in setOf("post_type", "language")) {
                        put(key, value.toString())
                    }
                }
            }

            val chunkCount = documentChunkingService.chunkAndStoreDocument(
                documentId = documentId,
                title = contentDoc.title,
                content = contentDoc.content,
                metadata = enrichedMetadata
            )
            logger.info { "[$tenantId] Created $chunkCount chunks for document $documentId" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to ingest document: ${contentDoc.title}" }
            throw ContentIngestionException("Failed to ingest document: ${e.message}", e)
        }
    }

    /**
     * Semantic search using pgvector with optional re-ranking
     * Uses LangChain4j's PgVectorEmbeddingStore for vector search
     *
     * Two-stage retrieval:
     * 1. Bi-encoder (embeddings) retrieves segment candidates quickly
     * 2. Cross-encoder re-ranks for accuracy (if enabled)
     */
    fun search(
        query: String,
        numResults: Int = 5,
        minConfidence: Double,
        language: String? = null
    ): List<SearchResult> {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Searching: $query (limit: $numResults, minConfidence: $minConfidence)" }

        try {
            // Get tenant-specific embedding store
            val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()

            // PERFORMANCE: Generate query embedding - typically takes ~0.3-1s
            val embeddingStart = System.currentTimeMillis()
            val queryEmbedding = embeddingModel.embed(query).content()
            val embeddingTime = System.currentTimeMillis() - embeddingStart
            logger.info { "[$tenantId] Query embedding generated in ${embeddingTime}ms" }

            // Stage 1: Retrieve segment candidates
            // Strategy: Retrieve 3x candidates for reranking to improve quality
            // Cross-encoder reranking (300-500ms) is faster than LLM processing large context
            val useReranking = sovereignragProperties.knowledgeGraph.useReranking
            val candidateCount = numResults * 3  // Get 3x candidates for reranking

            logger.debug { "[$tenantId] Retrieving $candidateCount candidates (reranking: $useReranking)" }

            // PERFORMANCE: Search using LangChain4j's PgVectorEmbeddingStore - typically takes ~100-300ms
            val searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(candidateCount)
                .minScore(minConfidence)
                .build()

            val vectorSearchStart = System.currentTimeMillis()
            val searchResult = embeddingStore.search(searchRequest)
            val vectorSearchTime = System.currentTimeMillis() - vectorSearchStart
            logger.info { "[$tenantId] Vector search completed in ${vectorSearchTime}ms" }

            logger.info { "[$tenantId] Found ${searchResult.matches().size} candidates" }

            // Convert to SearchResult
            val candidates = searchResult.matches().map { match ->
                val segment = match.embedded()
                val metadata = segment?.metadata()?.toMap() ?: emptyMap()

                SearchResult(
                    uuid = match.embeddingId(),
                    fact = segment?.text() ?: "",
                    confidence = match.score(),
                    source = metadata["url"] as? String ?: "",
                    validAt = LocalDateTime.now(), // TODO: Get from metadata if available
                    metadata = metadata.mapValues { it.value.toString() }
                )
            }

            // Stage 2: Re-rank with cross-encoder (uses parallel coroutines internally)
            val finalResults = if (useReranking && candidates.isNotEmpty()) {
                logger.info { "[$tenantId] Re-ranking ${candidates.size} candidates with cross-encoder" }
                // PERFORMANCE: Re-ranking typically takes ~300-500ms depending on candidate count
                // Trade-off: Reranking overhead is much smaller than LLM processing large context
                val rerankStart = System.currentTimeMillis()
                val reranked = kotlinx.coroutines.runBlocking {
                    rerankerService.rerank(query, candidates)
                }
                val rerankTime = System.currentTimeMillis() - rerankStart
                logger.info { "[$tenantId] Re-ranking completed in ${rerankTime}ms, passing top $numResults to LLM" }
                reranked.take(numResults)
            } else {
                logger.info { "[$tenantId] Skipping re-ranking - using top ${numResults} by embedding score" }
                // Fallback: use embedding scores if reranker unavailable
                candidates.take(numResults)
            }

            // Log final results
            finalResults.take(3).forEachIndexed { index, result ->
                logger.info {
                    "[$tenantId] Result ${index + 1} [${String.format("%.4f", result.confidence)}]: " +
                    "${result.fact.take(100)}... [Source: ${result.source}]"
                }
            }

            logger.info { "[$tenantId] Returning ${finalResults.size} search results" }
            return finalResults

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Error performing search: $query" }
            throw e
        }
    }

    /**
     * Delete document by URL
     * Note: With LangChain4j, we would need to query and delete embeddings by metadata filter
     * This is a placeholder for now
     */
    fun deleteByUrl(url: String) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Delete by URL not yet implemented with LangChain4j PgVectorEmbeddingStore" }
        logger.warn { "[$tenantId] To delete documents, you'll need to manually delete from langchain4j_embeddings table" }
    }

    /**
     * Get statistics for current tenant
     */
    fun getStats(): Map<String, Any> {
        val tenantId = TenantContext.getCurrentTenant()

        return mapOf(
            "status" to "ok",
            "store_type" to "langchain4j-pgvector",
            "tenant_id" to tenantId,
            "embedding_dimensions" to 1024,
            "embedding_model" to "snowflake-arctic-embed2",
            "note" to "All data managed by LangChain4j PgVectorEmbeddingStore"
        )
    }

    /**
     * Extract text from uploaded file using Apache Tika
     */
    fun extractTextFromFile(file: MultipartFile): String {
        val tenantId = TenantContext.getCurrentTenantOrNull()
        logger.info { "[${tenantId ?: "no-tenant"}] Extracting text from file: ${file.originalFilename}" }

        return try {
            // Create Apache Tika document parser
            val parser: DocumentParser = ApacheTikaDocumentParser()

            // Parse the document from input stream
            val document = parser.parse(file.inputStream)

            // Return extracted text
            document.text()
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract text from file: ${file.originalFilename}" }
            throw IllegalArgumentException("Failed to parse file: ${e.message}", e)
        }
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    /**
     * Calculate cosine similarity between two vectors
     * Returns value between 0 (completely different) and 1 (identical)
     */
    private fun calculateCosineSimilarity(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "Vectors must have same dimensions" }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (normA == 0.0 || normB == 0.0) {
            0.0
        } else {
            dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
        }
    }
}

class ContentIngestionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
