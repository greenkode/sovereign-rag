package ai.sovereignrag.core.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

// Stats and Analytics
data class StatsResponse(
    @JsonProperty("total_entities")
    val totalEntities: Long,
    @JsonProperty("total_episodes")
    val totalEpisodes: Long,
    @JsonProperty("total_relationships")
    val totalRelationships: Long,
    @JsonProperty("recent_ingestions")
    val recentIngestions: Int
)

// Entity Management
data class EntityDto(
    val uuid: String,
    val name: String,
    val summary: String?,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("relationship_count")
    val relationshipCount: Int = 0
)

data class EntityListResponse(
    val entities: List<EntityDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

// Episode Management
data class EpisodeDto(
    val uuid: String,
    val name: String,
    val content: String,
    val source: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime
)

data class EpisodeListResponse(
    val episodes: List<EpisodeDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

// Relationship Management
data class RelationshipDto(
    val uuid: String,
    val fact: String,
    @JsonProperty("source_entity")
    val sourceEntity: String,
    @JsonProperty("target_entity")
    val targetEntity: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime
)

data class RelationshipListResponse(
    val relationships: List<RelationshipDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

// Knowledge Graph Visualization
data class GraphNode(
    val id: String,
    val label: String,
    val type: String // "entity" or "episode"
)

data class GraphEdge(
    val id: String,
    val source: String,
    val target: String,
    val label: String,
    val type: String // "relates_to" or "mentioned_in"
)

data class GraphVisualizationResponse(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)

// Configuration
data class ConfigResponse(
    @JsonProperty("ollama_model")
    val ollamaModel: String,
    @JsonProperty("embedding_model")
    val embeddingModel: String,
    @JsonProperty("min_confidence")
    val minConfidence: Double,
    @JsonProperty("low_confidence_threshold")
    val lowConfidenceThreshold: Double,
    @JsonProperty("enable_rag")
    val enableRag: Boolean,
    @JsonProperty("enable_general_knowledge")
    val enableGeneralKnowledge: Boolean
)
