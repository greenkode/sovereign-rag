package ai.sovereignrag.knowledgebase.knowledgesource.controller

import ai.sovereignrag.commons.security.IsMerchant
import ai.sovereignrag.knowledgebase.knowledgesource.dto.KnowledgeSourceDto
import ai.sovereignrag.knowledgebase.knowledgesource.dto.KnowledgeSourceSummaryDto
import ai.sovereignrag.knowledgebase.knowledgesource.query.GetKnowledgeSourceQuery
import ai.sovereignrag.knowledgebase.knowledgesource.query.GetKnowledgeSourcesQuery
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/sources")
class KnowledgeSourceController(
    private val pipeline: Pipeline
) {

    @GetMapping
    @IsMerchant
    fun listKnowledgeSources(
        @PathVariable knowledgeBaseId: UUID,
        @PageableDefault(size = 20, sort = ["createdAt"]) pageable: Pageable
    ): ResponseEntity<Page<KnowledgeSourceSummaryDto>> {
        getOrganizationIdFromToken()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        log.debug { "Listing knowledge sources for knowledge base: $knowledgeBaseId" }

        val query = GetKnowledgeSourcesQuery(
            knowledgeBaseId = knowledgeBaseId,
            pageable = pageable
        )

        val result = pipeline.send(query)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{sourceId}")
    @IsMerchant
    fun getKnowledgeSource(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable sourceId: UUID
    ): ResponseEntity<KnowledgeSourceDto> {
        getOrganizationIdFromToken()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        log.debug { "Getting knowledge source: $sourceId for knowledge base: $knowledgeBaseId" }

        val query = GetKnowledgeSourceQuery(
            knowledgeBaseId = knowledgeBaseId,
            sourceId = sourceId
        )

        val result = pipeline.send(query)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(result)
    }

    private fun getOrganizationIdFromToken(): UUID? {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: return null
        return authentication.token.getClaimAsString("organization_id")?.let { UUID.fromString(it) }
    }
}
