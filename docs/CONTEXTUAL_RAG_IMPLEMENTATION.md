# Contextual RAG Implementation Plan

## Overview
Implement event-driven contextual understanding for the RAG system to handle high-level queries like "What is this website about?" or "Tell me about yourself".

## Problem Statement
Users of the chat widget may not know exactly what they're looking for and ask broad questions. The AI agent needs full understanding of the website's scope and context, not just individual page content.

## Solution Architecture
Event-driven context enrichment triggered on document ingestion, with multi-level chunking and synthetic document generation.

## Implementation Phases

### Phase 1: Foundation - Multi-Level Chunking & Metadata Enrichment
**Goal**: Enhance existing document ingestion with richer context

#### 1.1 Enhanced Metadata Structure
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/dto/ContentDto.kt`

Add new fields to capture WordPress context:
```kotlin
data class IngestRequest(
    val title: String,
    val content: String,
    val url: String? = null,
    val postType: String? = "page",
    val date: String? = null,

    // NEW: Contextual metadata
    val siteTitle: String? = null,
    val siteTagline: String? = null,
    val category: String? = null,
    val categoryDescription: String? = null,
    val tags: List<String>? = null,
    val excerpt: String? = null,
    val author: String? = null,
    val authorBio: String? = null,
    val relatedPosts: List<String>? = null,
    val breadcrumb: String? = null
)
```

**Status**: Not started
**Estimated effort**: 30 minutes

#### 1.2 Multi-Level Chunking Service
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/service/MultiLevelChunkingService.kt` (NEW)

Create service to generate three levels of chunks:
- **Micro chunks** (200-300 chars): Key facts, entities
- **Standard chunks** (800-1200 chars): Current implementation
- **Macro chunks** (3000-5000 chars): Section/page summaries

```kotlin
@Service
class MultiLevelChunkingService(
    private val embeddingModel: EmbeddingModel,
    private val llmService: LlmService
) {
    fun createMultiLevelChunks(
        content: String,
        title: String,
        metadata: Map<String, Any>
    ): MultiLevelChunks {
        return MultiLevelChunks(
            micro = extractKeyFacts(content),
            standard = standardChunk(content),
            macro = generateSummary(content, title)
        )
    }
}
```

**Status**: Not started
**Estimated effort**: 2 hours

### Phase 2: Synthetic Document Generation
**Goal**: Generate contextual documents on ingestion events

#### 2.1 Context Aggregation Service
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/service/ContextAggregationService.kt` (NEW)

Service to aggregate and generate contextual documents:
```kotlin
@Service
class ContextAggregationService(
    private val contentService: ContentService,
    private val llmService: LlmService,
    private val embeddingStore: TenantAwarePgVectorStoreFactory
) {

    /**
     * Generate site-wide overview document
     * Triggered when: First document ingested OR significant content changes
     */
    suspend fun generateSiteProfile()

    /**
     * Generate category summary
     * Triggered when: New document added to category OR category content updated
     */
    suspend fun generateCategorySummary(category: String)

    /**
     * Generate synthetic FAQs
     * Triggered when: New documents reach threshold (e.g., every 10 docs)
     */
    suspend fun generateCommonQuestions()

    /**
     * Extract and embed entity relationships
     * Triggered when: New document ingested
     */
    suspend fun extractAndEmbedEntities(document: ContentDocument)
}
```

**Status**: Not started
**Estimated effort**: 3 hours

#### 2.2 Site Profile Generator
**File**: Part of `ContextAggregationService.kt`

Generate comprehensive site overview:
```kotlin
suspend fun generateSiteProfile() {
    val tenantId = TenantContext.getCurrentTenant()

    // Query all documents to analyze
    val allDocuments = queryAllDocumentsMetadata()

    val prompt = """
    Analyze the following website content and create a comprehensive overview:

    Documents: ${allDocuments.size}
    Categories: ${allDocuments.map { it.category }.distinct()}
    Main topics: ${allDocuments.map { it.title }.take(20)}

    Generate:
    1. A clear description of what this website is about (2-3 sentences)
    2. Main services/products offered (bullet points)
    3. Target audience
    4. Key topics covered
    5. Unique value proposition
    """

    val siteProfile = llmService.generate(prompt)

    // Ingest as special document type
    contentService.ingest(ContentDocument(
        title = "Website Overview - ${tenantId}",
        content = siteProfile,
        source = "system-generated",
        metadata = mapOf(
            "document_type" to "site_profile",
            "priority" to "high",
            "generated_at" to Instant.now().toString()
        )
    ))
}
```

**Status**: Not started
**Estimated effort**: 2 hours

#### 2.3 Category Summary Generator
**File**: Part of `ContextAggregationService.kt`

Generate category-specific summaries:
```kotlin
suspend fun generateCategorySummary(category: String) {
    val documents = queryDocumentsByCategory(category)

    val prompt = """
    Summarize the following content from the "$category" category:

    ${documents.joinToString("\n") { "- ${it.title}: ${it.excerpt}" }}

    Create a comprehensive overview that explains:
    1. What this category covers
    2. Key topics/themes
    3. Main takeaways
    """

    val summary = llmService.generate(prompt)

    contentService.ingest(ContentDocument(
        title = "About: $category",
        content = summary,
        source = "system-generated",
        metadata = mapOf(
            "document_type" to "category_summary",
            "category" to category,
            "document_count" to documents.size.toString()
        )
    ))
}
```

**Status**: Not started
**Estimated effort**: 1.5 hours

#### 2.4 Synthetic FAQ Generator
**File**: Part of `ContextAggregationService.kt`

Extract common questions and generate answers:
```kotlin
suspend fun generateCommonQuestions() {
    val recentDocuments = queryRecentDocuments(limit = 50)

    val prompt = """
    Based on this website content, generate 10 common questions a visitor might ask:

    ${recentDocuments.joinToString("\n") { it.title }}

    For each question, provide a concise answer based on the available content.

    Include questions like:
    - "What is this website about?"
    - "What services do you offer?"
    - "Who is this for?"
    """

    val faqs = llmService.generateStructured<List<FAQ>>(prompt)

    faqs.forEach { faq ->
        contentService.ingest(ContentDocument(
            title = faq.question,
            content = faq.answer,
            source = "system-generated",
            metadata = mapOf(
                "document_type" to "synthetic_faq",
                "question_type" to "general"
            )
        ))
    }
}
```

**Status**: Not started
**Estimated effort**: 2 hours

### Phase 3: Event-Driven Triggers
**Goal**: Automatically trigger context generation on ingestion events

#### 3.1 Content Ingestion Event Publisher
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/event/ContentIngestionEvent.kt` (NEW)

```kotlin
data class ContentIngestionEvent(
    val tenantId: String,
    val documentId: UUID,
    val title: String,
    val category: String?,
    val postType: String,
    val timestamp: Instant
)

@Service
class ContentEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishIngestionEvent(event: ContentIngestionEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
```

**Status**: Not started
**Estimated effort**: 1 hour

#### 3.2 Context Enrichment Event Listener
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/event/ContextEnrichmentListener.kt` (NEW)

```kotlin
@Component
class ContextEnrichmentListener(
    private val contextAggregationService: ContextAggregationService,
    private val tenantRegistryService: TenantRegistryService
) {

    private val logger = KotlinLogging.logger {}

    @EventListener
    @Async
    fun onContentIngestion(event: ContentIngestionEvent) {
        TenantContext.setCurrentTenant(event.tenantId)

        try {
            logger.info { "[${event.tenantId}] Content ingestion event received for: ${event.title}" }

            // 1. Update category summary if category exists
            event.category?.let {
                contextAggregationService.generateCategorySummary(it)
            }

            // 2. Check if we should regenerate site profile
            val documentCount = getDocumentCount(event.tenantId)
            if (shouldRegenerateSiteProfile(documentCount)) {
                contextAggregationService.generateSiteProfile()
            }

            // 3. Generate FAQs periodically
            if (documentCount % 10 == 0) {
                contextAggregationService.generateCommonQuestions()
            }

        } finally {
            TenantContext.clear()
        }
    }

    private fun shouldRegenerateSiteProfile(count: Int): Boolean {
        // Regenerate on first doc, then every 25 docs
        return count == 1 || count % 25 == 0
    }
}
```

**Status**: Not started
**Estimated effort**: 2 hours

#### 3.3 Update IngestDocumentCommandHandler
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/command/IngestDocumentCommandHandler.kt`

Add event publishing:
```kotlin
@CommandHandler
class IngestDocumentCommandHandler(
    // ... existing dependencies
    private val contentEventPublisher: ContentEventPublisher
) {
    override fun handle(command: IngestDocumentCommand): IngestResponse {
        // ... existing ingestion logic

        // NEW: Publish event for context enrichment
        contentEventPublisher.publishIngestionEvent(
            ContentIngestionEvent(
                tenantId = TenantContext.getCurrentTenant(),
                documentId = documentId,
                title = command.title,
                category = command.category,
                postType = command.postType ?: "page",
                timestamp = Instant.now()
            )
        )

        return IngestResponse(/* ... */)
    }
}
```

**Status**: Not started
**Estimated effort**: 30 minutes

### Phase 4: Enhanced Retrieval
**Goal**: Detect and handle high-level queries differently

#### 4.1 Query Classifier
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/service/QueryClassifier.kt` (NEW)

```kotlin
@Service
class QueryClassifier {

    private val highLevelPatterns = listOf(
        "what.*this.*about",
        "tell me about",
        "what.*you.*do",
        "who are you",
        "what.*website.*about",
        "what.*offer",
        "what.*services"
    )

    fun isHighLevelQuery(query: String): Boolean {
        val normalized = query.lowercase()
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
    CONTEXTUAL,  // About site, services, general info
    SPECIFIC     // Specific question about content
}
```

**Status**: Not started
**Estimated effort**: 1 hour

#### 4.2 Contextual Retrieval Strategy
**File**: `core-ms/core-ai/src/main/kotlin/nl/compilot/ai/content/retriever/ContextualContentRetriever.kt` (NEW)

```kotlin
@Component
class ContextualContentRetriever(
    private val embeddingModel: EmbeddingModel,
    private val pgVectorStoreFactory: TenantAwarePgVectorStoreFactory,
    private val queryClassifier: QueryClassifier
) : ContentRetriever {

    override fun retrieve(query: Query): List<Content> {
        val queryType = queryClassifier.classifyQuery(query.text())

        return when (queryType) {
            QueryType.CONTEXTUAL -> retrieveContextualContent(query)
            QueryType.SPECIFIC -> retrieveSpecificContent(query)
        }
    }

    private fun retrieveContextualContent(query: Query): List<Content> {
        val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()
        val queryEmbedding = embeddingModel.embed(query.text()).content()

        // Priority search for synthetic/contextual docs
        val contextualDocs = searchWithFilter(
            embedding = queryEmbedding,
            filter = mapOf("documentType" to listOf("site_profile", "category_summary", "synthetic_faq")),
            maxResults = 3
        )

        // Add some specific content for details
        val specificDocs = searchWithFilter(
            embedding = queryEmbedding,
            filter = emptyMap(),
            maxResults = 2
        )

        return contextualDocs + specificDocs
    }

    private fun retrieveSpecificContent(query: Query): List<Content> {
        // Standard retrieval
        // ... existing logic
    }
}
```

**Status**: Not started
**Estimated effort**: 2 hours

### Phase 5: WordPress Plugin Updates
**Goal**: Send enriched metadata from WordPress

#### 5.1 Update WordPress Content Sync
**File**: `wordpress-plugin/compilot-ai-plugin/includes/content-sync.php`

```php
function compilot_prepare_post_data($post) {
    // Existing fields
    $data = [
        'title' => $post->post_title,
        'content' => $post->post_content,
        'url' => get_permalink($post->ID),
        'postType' => $post->post_type,
        'date' => $post->post_date,
    ];

    // NEW: Contextual metadata
    $data['siteTitle'] = get_bloginfo('name');
    $data['siteTagline'] = get_bloginfo('description');

    // Category info
    $categories = get_the_category($post->ID);
    if (!empty($categories)) {
        $category = $categories[0];
        $data['category'] = $category->name;
        $data['categoryDescription'] = category_description($category->term_id);
    }

    // Tags
    $tags = get_the_tags($post->ID);
    if ($tags) {
        $data['tags'] = array_map(function($tag) {
            return $tag->name;
        }, $tags);
    }

    // Excerpt
    $data['excerpt'] = get_the_excerpt($post->ID);

    // Author info
    $author_id = $post->post_author;
    $data['author'] = get_the_author_meta('display_name', $author_id);
    $data['authorBio'] = get_the_author_meta('description', $author_id);

    // Related posts
    $related = get_posts([
        'category__in' => wp_get_post_categories($post->ID),
        'numberposts' => 5,
        'post__not_in' => [$post->ID]
    ]);
    $data['relatedPosts'] = array_map(function($p) {
        return get_permalink($p->ID);
    }, $related);

    return $data;
}
```

**Status**: Not started
**Estimated effort**: 1.5 hours

## Database Schema Changes

### New metadata fields in langchain4j_embeddings
No schema changes needed - all stored in JSONB `metadata` column:
- `document_type`: "page" | "post" | "site_profile" | "category_summary" | "synthetic_faq"
- `priority`: "high" | "normal" | "low"
- `category`: String
- `tags`: JSON array
- `generated_at`: ISO timestamp
- `source`: "wordpress" | "system-generated"

## Implementation Order

### Sprint 1: Foundation (1 week)
1. Enhanced metadata structure (1.1)
2. Multi-level chunking service (1.2)
3. Update WordPress plugin (5.1)

### Sprint 2: Context Generation (1 week)
4. Context aggregation service structure (2.1)
5. Site profile generator (2.2)
6. Category summary generator (2.3)

### Sprint 3: Event-Driven Architecture (1 week)
7. Content ingestion events (3.1)
8. Event listener (3.2)
9. Update command handlers (3.3)

### Sprint 4: Enhanced Retrieval (1 week)
10. Query classifier (4.1)
11. Contextual retrieval strategy (4.2)
12. Synthetic FAQ generator (2.4)

## Testing Strategy

### Unit Tests
- Multi-level chunking logic
- Query classification patterns
- Event publishing/listening

### Integration Tests
- End-to-end ingestion with context generation
- Contextual vs specific query retrieval
- WordPress metadata extraction

### Manual Testing Scenarios
1. Ingest 5 pages in "Products" category → Verify category summary created
2. Ask "What is this website about?" → Verify site profile returned
3. Ingest 10th document → Verify FAQs generated
4. Ask specific question → Verify specific content returned

## Success Metrics

1. **Coverage**: Site profile exists for all tenants with >5 documents
2. **Accuracy**: High-level queries return contextual docs in top 3 results
3. **Performance**: Context generation completes within 10 seconds
4. **User Satisfaction**: Reduction in "I don't know" responses for general questions

## Rollback Plan

All new functionality is additive:
- Can disable event listeners via feature flag
- Synthetic documents marked with `document_type` - can be deleted
- Multi-level chunking backwards compatible

## Future Enhancements

- Knowledge graph extraction
- Temporal context ("What's new this month?")
- Cross-tenant learning (with privacy controls)
- User feedback loop for synthetic content quality
