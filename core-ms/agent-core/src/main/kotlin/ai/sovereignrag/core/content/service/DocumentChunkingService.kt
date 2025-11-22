package ai.sovereignrag.core.content.service

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import mu.KotlinLogging
import nl.compilot.ai.content.store.TenantAwarePgVectorStoreFactory
import nl.compilot.ai.commons.tenant.TenantContext
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Service for chunking documents using LangChain4j's DocumentSplitter
 * Now uses PgVectorEmbeddingStore for automatic schema management
 *
 * Chunking strategy:
 * - 1000 character chunks
 * - 200 character overlap
 */
@Service
class DocumentChunkingService(
    private val embeddingModel: EmbeddingModel,
    private val pgVectorStoreFactory: TenantAwarePgVectorStoreFactory
) {

    companion object {
        const val CHUNK_SIZE = 1000
        const val CHUNK_OVERLAP = 200
    }

    private val documentSplitter: DocumentSplitter = DocumentSplitters.recursive(
        CHUNK_SIZE,
        CHUNK_OVERLAP
    )

    /**
     * Chunk a document and store segments with embeddings
     * Uses PgVectorEmbeddingStore for automatic storage and schema management
     *
     * @param documentId Parent document ID
     * @param title Document title
     * @param content Full document content
     * @param metadata Additional metadata
     * @return Number of chunks created
     */
    fun chunkAndStoreDocument(
        documentId: UUID,
        title: String,
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Int {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Chunking document $documentId: $title" }

        // Get tenant-specific embedding store
        val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()

        // Create LangChain4j Document
        val doc = Document.from(
            content,
            Metadata.from(metadata + mapOf(
                "documentId" to documentId.toString(),
                "title" to title
            ))
        )

        // Split document into chunks
        val segments = documentSplitter.split(doc)
        logger.info { "[$tenantId] Split document into ${segments.size} segments" }

        if (segments.isEmpty()) {
            logger.warn { "[$tenantId] No segments created for document $documentId" }
            return 0
        }

        // Add metadata to each segment
        val enrichedSegments = segments.mapIndexed { index, segment ->
            TextSegment.from(
                segment.text(),
                Metadata.from(segment.metadata().toMap() + mapOf(
                    "segmentIndex" to index,
                    "tokenCount" to estimateTokenCount(segment.text()),
                    "chunkIndex" to index  // For compatibility
                ))
            )
        }

        // Generate embeddings for all segments
        logger.debug { "[$tenantId] Generating embeddings for ${enrichedSegments.size} segments" }
        val embeddings = embeddingModel.embedAll(enrichedSegments).content()

        // Store segments with embeddings using PgVectorEmbeddingStore
        logger.debug { "[$tenantId] Storing segments with embeddings in PgVector" }
        embeddingStore.addAll(embeddings, enrichedSegments)

        logger.info { "[$tenantId] Successfully stored ${segments.size} segments for document $documentId" }
        return segments.size
    }

    /**
     * Rough token count estimation (1 token ≈ 4 characters for English)
     */
    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4.0).toInt()
    }
}
