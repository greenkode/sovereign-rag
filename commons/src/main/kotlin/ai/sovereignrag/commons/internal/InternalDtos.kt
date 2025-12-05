package ai.sovereignrag.commons.internal

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class OrganizationResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    @JsonProperty("database_name")
    val databaseName: String?,
    @JsonProperty("database_created")
    val databaseCreated: Boolean,
    @JsonProperty("max_knowledge_bases")
    val maxKnowledgeBases: Int
)

data class UpdateOrganizationDatabaseRequest(
    @JsonProperty("database_name")
    val databaseName: String
)

data class CreateKBOAuthClientRequest(
    @JsonProperty("organization_id")
    val organizationId: UUID,
    @JsonProperty("knowledge_base_id")
    val knowledgeBaseId: String,
    val name: String
)

data class CreateKBOAuthClientResponse(
    val success: Boolean,
    @JsonProperty("client_id")
    val clientId: String? = null,
    @JsonProperty("client_secret")
    val clientSecret: String? = null,
    @JsonProperty("knowledge_base_id")
    val knowledgeBaseId: String? = null,
    val message: String? = null
)

data class RevokeKBOAuthClientResponse(
    val success: Boolean,
    val message: String? = null
)

data class RotateSecretResponse(
    val success: Boolean,
    @JsonProperty("client_id")
    val clientId: String? = null,
    @JsonProperty("client_secret")
    val clientSecret: String? = null,
    val message: String? = null
)
