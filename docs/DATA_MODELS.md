# Data Models and Database Schema

## Overview
This document defines the complete data model for the Sovereign RAG system based on the Progress Agentic RAG product analysis.

---

## 1. CORE ENTITIES

### 1.1 KnowledgeBox (Tenant)
Primary entity representing a tenant's knowledge base.

```kotlin
@Entity
@Table(name = "knowledge_boxes", schema = "master")
data class KnowledgeBox(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val slug: String,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false)
    val zone: String = "europe-1", // Geographic region

    @Column(nullable = false)
    val status: KnowledgeBoxStatus = KnowledgeBoxStatus.PRIVATE,

    @Column(name = "enable_hidden_resources")
    val enableHiddenResources: Boolean = false,

    @Column(name = "allowed_origins", columnDefinition = "TEXT[]")
    val allowedOrigins: List<String> = emptyList(),

    @Column(name = "allowed_ip_addresses", columnDefinition = "TEXT[]")
    val allowedIpAddresses: List<String> = emptyList(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID
)

enum class KnowledgeBoxStatus {
    PRIVATE,
    PUBLIC
}
```

### 1.2 Resource (Document)
Represents uploaded content in the tenant database.

```kotlin
@Entity
@Table(name = "resources", schema = "tenant")
data class Resource(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val summary: String? = null,

    @Column(nullable = false)
    val type: ResourceType,

    @Column(name = "source_type", nullable = false)
    val sourceType: SourceType,

    @Column(name = "source_url", columnDefinition = "TEXT")
    val sourceUrl: String? = null,

    @Column(name = "file_path", columnDefinition = "TEXT")
    val filePath: String? = null,

    @Column(name = "mime_type")
    val mimeType: String? = null,

    @Column(name = "file_size")
    val fileSize: Long? = null,

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    val thumbnailUrl: String? = null,

    @Column(nullable = false)
    val status: ResourceStatus = ResourceStatus.PROCESSING,

    @Column(name = "is_hidden")
    val isHidden: Boolean = false,

    @Column(name = "metadata", columnDefinition = "JSONB")
    val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "indexed_at")
    val indexedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class ResourceType {
    DOCUMENT,
    WEB_PAGE,
    TEXT,
    QA_PAIR,
    FOLDER,
    SITEMAP
}

enum class SourceType {
    UPLOAD,
    LINK,
    SYNC_FOLDER,
    SYNC_RSS,
    SYNC_SITEMAP,
    MANUAL_TEXT
}

enum class ResourceStatus {
    PROCESSING,
    INDEXED,
    FAILED,
    DELETED
}
```

### 1.3 TextBlock (Chunk)
Chunked segments of resources stored with embeddings.

```kotlin
@Entity
@Table(name = "text_blocks", schema = "tenant")
data class TextBlock(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "resource_id", nullable = false)
    val resourceId: UUID,

    @Column(name = "block_index", nullable = false)
    val blockIndex: Int, // Order within the resource

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Column(name = "content_hash", nullable = false)
    val contentHash: String, // For deduplication

    @Column(name = "token_count")
    val tokenCount: Int? = null,

    @Column(name = "start_position")
    val startPosition: Int? = null, // Character offset in original

    @Column(name = "end_position")
    val endPosition: Int? = null,

    @Column(name = "heading_text")
    val headingText: String? = null, // Parent section heading

    @Column(name = "metadata", columnDefinition = "JSONB")
    val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "labels", columnDefinition = "TEXT[]")
    val labels: List<String> = emptyList(),

    @Column(name = "embedding_status", nullable = false)
    val embeddingStatus: EmbeddingStatus = EmbeddingStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class EmbeddingStatus {
    PENDING,
    EMBEDDED,
    FAILED
}
```

### 1.4 Embedding (Vector Storage)
Uses pgvector extension - stored in langchain4j_embeddings table.

```sql
CREATE TABLE langchain4j_embeddings (
    id UUID PRIMARY KEY,
    embedding vector(1024), -- Dimension configurable per embedding model
    text TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX ON langchain4j_embeddings USING ivfflat (embedding vector_cosine_ops);
```

---

## 2. AI CONFIGURATION

### 2.1 AIModelConfiguration
Stores selected models for various tasks.

```kotlin
@Entity
@Table(name = "ai_model_configurations", schema = "master")
data class AIModelConfiguration(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(name = "task_type", nullable = false)
    val taskType: AITaskType,

    @Column(name = "provider", nullable = false)
    val provider: AIProvider,

    @Column(name = "model_name", nullable = false)
    val modelName: String,

    @Column(name = "model_parameters", columnDefinition = "JSONB")
    val modelParameters: Map<String, Any> = emptyMap(),

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class AITaskType {
    ANSWER_GENERATION,
    ON_DEMAND_SUMMARIZATION,
    EMBEDDINGS,
    EXTRACT_AND_SPLIT,
    ANONYMIZATION,
    QUERY_REPHRASING,
    SEMANTIC_RERANKING
}

enum class AIProvider {
    OPENAI_AZURE,
    OPENAI,
    ANTHROPIC,
    GOOGLE,
    MISTRAL,
    OLLAMA // For local/self-hosted
}
```

---

## 3. SEARCH CONFIGURATION

### 3.1 SearchConfiguration
Saved search configurations and widget settings.

```kotlin
@Entity
@Table(name = "search_configurations", schema = "master")
data class SearchConfiguration(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    // Search Options
    @Column(name = "use_search_results", nullable = false)
    val useSearchResults: Boolean = true,

    @Column(name = "show_filter_button")
    val showFilterButton: Boolean = false,

    @Column(name = "preselected_filters", columnDefinition = "JSONB")
    val preselectedFilters: Map<String, Any> = emptyMap(),

    @Column(name = "rephrase_query", nullable = false)
    val rephraseQuery: Boolean = true,

    @Column(name = "custom_rephrase_prompt", columnDefinition = "TEXT")
    val customRephrasePrompt: String? = null,

    @Column(name = "semantic_reranking", nullable = false)
    val semanticReranking: Boolean = true,

    @Column(name = "reciprocal_rank_fusion")
    val reciprocalRankFusion: Boolean = false,

    // Generative Answer Options
    @Column(name = "generate_answer", nullable = false)
    val generateAnswer: Boolean = true,

    @Column(name = "specific_model_id")
    val specificModelId: UUID? = null,

    @Column(name = "use_specific_prompt")
    val useSpecificPrompt: Boolean = false,

    @Column(name = "specific_prompt", columnDefinition = "TEXT")
    val specificPrompt: String? = null,

    @Column(name = "use_specific_system_prompt")
    val useSpecificSystemPrompt: Boolean = false,

    @Column(name = "specific_system_prompt", columnDefinition = "TEXT")
    val specificSystemPrompt: String? = null,

    @Column(name = "ask_specific_resource_id")
    val askSpecificResourceId: UUID? = null,

    @Column(name = "show_reasoning")
    val showReasoning: Boolean = false,

    @Column(name = "limit_token_consumption")
    val limitTokenConsumption: Boolean = false,

    @Column(name = "max_tokens")
    val maxTokens: Int? = null,

    @Column(name = "prefer_markdown_format")
    val preferMarkdownFormat: Boolean = true,

    // Result Display Options
    @Column(name = "display_results", nullable = false)
    val displayResults: Boolean = true,

    @Column(name = "display_mode", nullable = false)
    val displayMode: DisplayMode = DisplayMode.SHOW_CITATIONS,

    @Column(name = "citation_threshold")
    val citationThreshold: Double? = null,

    @Column(name = "display_metadata")
    val displayMetadata: Boolean = true,

    @Column(name = "display_thumbnails", nullable = false)
    val displayThumbnails: Boolean = true,

    @Column(name = "limit_top_k")
    val limitTopK: Int? = null,

    @Column(name = "show_attached_images")
    val showAttachedImages: Boolean = false,

    @Column(name = "display_field_list")
    val displayFieldList: Boolean = false,

    @Column(name = "display_relations")
    val displayRelations: Boolean = false,

    // User Intent Routing
    @Column(name = "use_routing")
    val useRouting: Boolean = false,

    @Column(name = "routing_prompts", columnDefinition = "JSONB")
    val routingPrompts: List<Map<String, String>> = emptyList(),

    @Column(name = "is_default")
    val isDefault: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class DisplayMode {
    SHOW_ALL_RESULTS,
    SHOW_CITATIONS,
    SHOW_LLM_CITATIONS
}
```

---

## 4. WIDGETS

### 4.1 Widget
Embeddable search widget configurations.

```kotlin
@Entity
@Table(name = "widgets", schema = "master")
data class Widget(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(name = "search_configuration_id", nullable = false)
    val searchConfigurationId: UUID,

    // Widget Appearance
    @Column(name = "search_bar_placeholder")
    val searchBarPlaceholder: String = "Type your question here",

    @Column(name = "chat_bar_placeholder")
    val chatBarPlaceholder: String = "Let's talk",

    @Column(name = "insufficient_data_message", columnDefinition = "TEXT")
    val insufficientDataMessage: String? = null,

    // Widget Style
    @Column(name = "widget_style", nullable = false)
    val widgetStyle: WidgetStyle = WidgetStyle.EMBEDDED_IN_PAGE,

    @Column(name = "widget_theme", nullable = false)
    val widgetTheme: WidgetTheme = WidgetTheme.LIGHT,

    // Chat History
    @Column(name = "enable_chat_history", nullable = false)
    val enableChatHistory: Boolean = true,

    @Column(name = "persist_chat_history", nullable = false)
    val persistChatHistory: Boolean = false,

    // Feedback
    @Column(name = "feedback_mode", nullable = false)
    val feedbackMode: FeedbackMode = FeedbackMode.GLOBAL_FEEDBACK,

    @Column(name = "copy_button_disclaimer", columnDefinition = "TEXT")
    val copyButtonDisclaimer: String? = null,

    // Text Display
    @Column(name = "collapse_text_blocks")
    val collapseTextBlocks: Boolean = false,

    @Column(name = "citations_expanded")
    val citationsExpanded: Boolean = true,

    @Column(name = "embed_code", columnDefinition = "TEXT")
    val embedCode: String? = null, // Generated JavaScript snippet

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class WidgetStyle {
    EMBEDDED_IN_PAGE,
    CHAT_MODE,
    POPUP_MODAL
}

enum class WidgetTheme {
    LIGHT,
    DARK
}

enum class FeedbackMode {
    NO_FEEDBACK,
    GLOBAL_FEEDBACK,
    DETAILED_FEEDBACK
}
```

---

## 5. SYNCHRONIZATION

### 5.1 SyncConfiguration
External data source synchronization settings.

```kotlin
@Entity
@Table(name = "sync_configurations", schema = "master")
data class SyncConfiguration(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(name = "sync_type", nullable = false)
    val syncType: SyncType,

    @Column(name = "sync_agent_type", nullable = false)
    val syncAgentType: SyncAgentType,

    @Column(name = "source_config", columnDefinition = "JSONB", nullable = false)
    val sourceConfig: Map<String, Any>, // Type-specific configuration

    @Column(name = "schedule_cron")
    val scheduleCron: String? = null, // Cron expression for auto-sync

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "last_sync_at")
    val lastSyncAt: Instant? = null,

    @Column(name = "last_sync_status")
    val lastSyncStatus: SyncStatus? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class SyncType {
    FOLDER,
    RSS,
    SITEMAP
}

enum class SyncAgentType {
    DESKTOP_AGENT,
    REMOTE_SERVER
}

enum class SyncStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED
}
```

### 5.2 SyncLog
Track synchronization history.

```kotlin
@Entity
@Table(name = "sync_logs", schema = "master")
data class SyncLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "sync_configuration_id", nullable = false)
    val syncConfigurationId: UUID,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "completed_at")
    val completedAt: Instant? = null,

    @Column(nullable = false)
    val status: SyncStatus,

    @Column(name = "resources_added")
    val resourcesAdded: Int = 0,

    @Column(name = "resources_updated")
    val resourcesUpdated: Int = 0,

    @Column(name = "resources_deleted")
    val resourcesDeleted: Int = 0,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "error_details", columnDefinition = "JSONB")
    val errorDetails: Map<String, Any>? = null
)
```

---

## 6. QUERY & ACTIVITY TRACKING

### 6.1 Query (Activity Log)
Track all queries for analytics and REMI evaluation.

```kotlin
@Entity
@Table(name = "queries", schema = "tenant")
data class Query(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "session_id")
    val sessionId: UUID? = null, // Group related queries in a chat session

    @Column(name = "widget_id")
    val widgetId: UUID? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    val query: String,

    @Column(name = "rephrased_query", columnDefinition = "TEXT")
    val rephrasedQuery: String? = null,

    @Column(name = "search_configuration_id", nullable = false)
    val searchConfigurationId: UUID,

    @Column(name = "generated_answer", columnDefinition = "TEXT")
    val generatedAnswer: String? = null,

    @Column(name = "answer_model")
    val answerModel: String? = null,

    @Column(name = "reasoning_steps", columnDefinition = "JSONB")
    val reasoningSteps: List<String>? = null,

    @Column(name = "search_results", columnDefinition = "JSONB")
    val searchResults: List<Map<String, Any>> = emptyList(),

    @Column(name = "citations", columnDefinition = "JSONB")
    val citations: List<Map<String, Any>> = emptyList(),

    @Column(name = "response_time_ms")
    val responseTimeMs: Long? = null,

    @Column(name = "tokens_consumed")
    val tokensConsumed: Int? = null,

    @Column(name = "user_ip")
    val userIp: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
```

### 6.2 QueryFeedback
User feedback on answers.

```kotlin
@Entity
@Table(name = "query_feedback", schema = "tenant")
data class QueryFeedback(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "query_id", nullable = false)
    val queryId: UUID,

    @Column(name = "feedback_type", nullable = false)
    val feedbackType: FeedbackType,

    @Column(nullable = false)
    val rating: FeedbackRating, // Thumbs up/down or star rating

    @Column(columnDefinition = "TEXT")
    val comment: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class FeedbackType {
    ANSWER_ONLY,
    ANSWER_AND_RESULTS
}

enum class FeedbackRating {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}
```

### 6.3 RAGMetrics
REMI evaluation scores.

```kotlin
@Entity
@Table(name = "rag_metrics", schema = "tenant")
data class RAGMetrics(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "query_id", nullable = false, unique = true)
    val queryId: UUID,

    @Column(name = "answer_relevance")
    val answerRelevance: Double? = null, // 0.0 to 1.0

    @Column(name = "context_relevance")
    val contextRelevance: Double? = null, // 0.0 to 1.0

    @Column(name = "groundedness")
    val groundedness: Double? = null, // 0.0 to 1.0

    @Column(name = "computed_at", nullable = false)
    val computedAt: Instant = Instant.now()
)
```

---

## 7. LABELS & CLASSIFICATION

### 7.1 LabelSet
Define label taxonomies.

```kotlin
@Entity
@Table(name = "label_sets", schema = "master")
data class LabelSet(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(name = "apply_to", nullable = false)
    val applyTo: LabelApplyTo,

    @Column(name = "is_exclusive", nullable = false)
    val isExclusive: Boolean = false, // Only one label per resource/block

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class LabelApplyTo {
    RESOURCES,
    TEXT_BLOCKS
}
```

### 7.2 Label
Individual labels within label sets.

```kotlin
@Entity
@Table(name = "labels", schema = "master")
data class Label(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "label_set_id", nullable = false)
    val labelSetId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false)
    val color: String = "#3B82F6", // Hex color code

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
```

---

## 8. USER MANAGEMENT

### 8.1 User
User accounts with knowledge box access.

```kotlin
@Entity
@Table(name = "users", schema = "master")
data class User(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
```

### 8.2 KnowledgeBoxUser
Many-to-many relationship with roles.

```kotlin
@Entity
@Table(name = "knowledge_box_users", schema = "master")
data class KnowledgeBoxUser(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val role: UserRole,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class UserRole {
    READER,   // Read-only access
    WRITER,   // Can upload and modify content
    MANAGER   // Full administrative access
}
```

### 8.3 APIKey
API keys for programmatic access.

```kotlin
@Entity
@Table(name = "api_keys", schema = "master")
data class APIKey(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_box_id", nullable = false)
    val knowledgeBoxId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(name = "key_hash", nullable = false, unique = true)
    val keyHash: String, // BCrypt hash

    @Column(name = "key_prefix", nullable = false)
    val keyPrefix: String, // First 8 chars for identification

    @Column(nullable = false)
    val role: UserRole,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "last_used_at")
    val lastUsedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "created_by", nullable = false)
    val createdBy: UUID
)
```

---

## 9. RELATIONSHIPS

### Entity Relationship Diagram (ERD)

```
┌──────────────────┐
│  KnowledgeBox    │
│  (master schema) │
└────────┬─────────┘
         │
         ├─────────────┬──────────────┬──────────────┬──────────────────┐
         │             │              │              │                  │
         ▼             ▼              ▼              ▼                  ▼
┌─────────────┐ ┌─────────────┐ ┌─────────┐ ┌──────────────┐ ┌──────────────┐
│   Widget    │ │   Search    │ │ AIModel │ │     Sync     │ │  LabelSet    │
│             │ │   Config    │ │ Config  │ │Configuration │ │              │
└──────┬──────┘ └─────────────┘ └─────────┘ └──────┬───────┘ └──────┬───────┘
       │                                            │                 │
       │                                            ▼                 ▼
       │                                     ┌─────────────┐   ┌─────────┐
       │                                     │  SyncLog    │   │  Label  │
       │                                     └─────────────┘   └─────────┘
       │
       │        ┌──────────────────────────────────────────────────┐
       │        │         Tenant Schema (separate database)        │
       │        │                                                  │
       │        │  ┌──────────────┐                                │
       │        │  │   Resource   │                                │
       │        │  └──────┬───────┘                                │
       │        │         │                                        │
       │        │         ├────────────────┬─────────────┐         │
       │        │         ▼                ▼             ▼         │
       │        │  ┌─────────────┐  ┌──────────┐ ┌─────────────┐  │
       │        │  │  TextBlock  │  │ Embedding│ │    Query    │  │
       │        │  └─────────────┘  │(pgvector)│ └──────┬──────┘  │
       │        │                   └──────────┘        │         │
       │        │                                       ├─────┬───┤
       │        │                                       ▼     ▼   ▼
       │        │                              ┌─────────┐ ┌──────┐ ┌───────┐
       │        │                              │Feedback │ │REMI  │ │Session│
       │        │                              └─────────┘ └──────┘ └───────┘
       │        └──────────────────────────────────────────────────┘
       │
       └─────────────────> (Widget queries tenant data)


┌──────────────────┐
│      User        │
│  (master schema) │
└────────┬─────────┘
         │
         ├────────────────┬──────────────┐
         ▼                ▼              ▼
┌──────────────────┐ ┌─────────┐ ┌─────────┐
│KnowledgeBoxUser  │ │ APIKey  │ │ Session │
└──────────────────┘ └─────────┘ └─────────┘
```

---

## 10. DATABASE INDICES

### Performance-Critical Indices

```sql
-- Master Schema Indices
CREATE INDEX idx_knowledge_boxes_slug ON knowledge_boxes(slug);
CREATE INDEX idx_knowledge_boxes_owner ON knowledge_boxes(owner_id);
CREATE INDEX idx_search_configs_kb ON search_configurations(knowledge_box_id);
CREATE INDEX idx_widgets_kb ON widgets(knowledge_box_id);
CREATE INDEX idx_sync_configs_kb ON sync_configurations(knowledge_box_id);
CREATE INDEX idx_api_keys_kb ON api_keys(knowledge_box_id);
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_kb_users_kb ON knowledge_box_users(knowledge_box_id);
CREATE INDEX idx_kb_users_user ON knowledge_box_users(user_id);

-- Tenant Schema Indices
CREATE INDEX idx_resources_type ON resources(type);
CREATE INDEX idx_resources_status ON resources(status);
CREATE INDEX idx_resources_created_at ON resources(created_at DESC);
CREATE INDEX idx_text_blocks_resource ON text_blocks(resource_id);
CREATE INDEX idx_text_blocks_hash ON text_blocks(content_hash);
CREATE INDEX idx_queries_session ON queries(session_id);
CREATE INDEX idx_queries_widget ON queries(widget_id);
CREATE INDEX idx_queries_created_at ON queries(created_at DESC);
CREATE INDEX idx_feedback_query ON query_feedback(query_id);
CREATE INDEX idx_metrics_query ON rag_metrics(query_id);

-- Full-text search indices
CREATE INDEX idx_resources_title_fts ON resources USING gin(to_tsvector('english', title));
CREATE INDEX idx_text_blocks_content_fts ON text_blocks USING gin(to_tsvector('english', content));

-- Vector similarity index (pgvector)
CREATE INDEX idx_embeddings_vector ON langchain4j_embeddings
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

---

## 11. DATA MIGRATION STRATEGY

### From Current Schema to New Schema

The current schema already has some of these entities. Key migrations needed:

1. **Add Widget table** - New entity
2. **Add SearchConfiguration table** - Expand current search config
3. **Add SyncConfiguration & SyncLog** - New for external syncing
4. **Add LabelSet & Label** - New classification system
5. **Add RAGMetrics** - New for REMI evaluation
6. **Add QueryFeedback** - New for user feedback
7. **Expand AIModelConfiguration** - Add more task types

### Migration Order:
1. Core entities (KnowledgeBox, User, Resource already exist)
2. AI configuration tables
3. Search configuration tables
4. Widget tables
5. Sync tables
6. Label tables
7. Analytics tables (Query, Feedback, Metrics)

---

## Next Documents

1. **API_SPECIFICATIONS.md** - REST API endpoints
2. **SEARCH_RAG_SPECS.md** - Search algorithm and RAG implementation
3. **WIDGET_EMBEDDING_SPECS.md** - JavaScript widget specifications
4. **IMPLEMENTATION_ROADMAP.md** - Phased development plan
