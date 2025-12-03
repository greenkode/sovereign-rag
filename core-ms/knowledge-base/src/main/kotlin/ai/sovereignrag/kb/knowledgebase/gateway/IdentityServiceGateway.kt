package ai.sovereignrag.kb.knowledgebase.gateway

import java.util.UUID

data class KBOAuthCredentials(
    val clientId: String,
    val clientSecret: String,
    val knowledgeBaseId: String
)

data class OrganizationInfo(
    val id: UUID,
    val name: String,
    val slug: String,
    val databaseName: String?,
    val databaseCreated: Boolean,
    val maxKnowledgeBases: Int
)

interface IdentityServiceGateway {
    fun createKBOAuthClient(organizationId: UUID, knowledgeBaseId: String, name: String): KBOAuthCredentials
    fun revokeKBOAuthClient(knowledgeBaseId: String)
    fun rotateKBOAuthClientSecret(knowledgeBaseId: String): KBOAuthCredentials
    fun getOrganization(organizationId: UUID): OrganizationInfo
    fun updateOrganizationDatabase(organizationId: UUID, databaseName: String)
}
