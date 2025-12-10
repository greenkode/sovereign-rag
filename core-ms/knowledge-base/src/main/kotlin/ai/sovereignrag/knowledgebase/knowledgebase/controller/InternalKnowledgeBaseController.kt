package ai.sovereignrag.knowledgebase.knowledgebase.controller

import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import ai.sovereignrag.knowledgebase.knowledgebase.service.OrganizationDatabaseRegistry
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal/knowledge-bases")
class InternalKnowledgeBaseController(
    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService,
    private val organizationDatabaseRegistry: OrganizationDatabaseRegistry
) {

    @GetMapping("/{knowledgeBaseId}/database-config")
    @Cacheable(cacheNames = ["knowledge_base_db_config"], key = "#knowledgeBaseId")
    fun getKnowledgeBaseDatabaseConfig(
        @PathVariable knowledgeBaseId: String
    ): KnowledgeBaseDatabaseConfigResponse {
        val kb = knowledgeBaseRegistryService.getKnowledgeBase(knowledgeBaseId)
        val databaseName = organizationDatabaseRegistry.getDatabaseName(kb.organizationId)

        return KnowledgeBaseDatabaseConfigResponse(
            knowledgeBaseId = kb.id,
            organizationId = kb.organizationId,
            regionCode = kb.regionCode,
            schemaName = kb.schemaName,
            databaseName = databaseName,
            embeddingModelId = kb.embeddingModelId
        )
    }

    @GetMapping("/{knowledgeBaseId}")
    @Cacheable(cacheNames = ["knowledge_base_info"], key = "#knowledgeBaseId")
    fun getKnowledgeBase(
        @PathVariable knowledgeBaseId: String
    ): KnowledgeBaseInfoResponse {
        val kb = knowledgeBaseRegistryService.getKnowledgeBase(knowledgeBaseId)

        return KnowledgeBaseInfoResponse(
            id = kb.id,
            organizationId = kb.organizationId,
            regionCode = kb.regionCode,
            schemaName = kb.schemaName,
            status = kb.status.name,
            embeddingModelId = kb.embeddingModelId
        )
    }
}

data class KnowledgeBaseDatabaseConfigResponse(
    val knowledgeBaseId: String,
    val organizationId: UUID,
    val regionCode: String,
    val schemaName: String,
    val databaseName: String,
    val embeddingModelId: String?
)

data class KnowledgeBaseInfoResponse(
    val id: String,
    val organizationId: UUID,
    val regionCode: String,
    val schemaName: String,
    val status: String,
    val embeddingModelId: String?
)
