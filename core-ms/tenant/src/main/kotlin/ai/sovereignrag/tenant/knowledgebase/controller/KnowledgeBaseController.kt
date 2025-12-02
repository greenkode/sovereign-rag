package ai.sovereignrag.tenant.knowledgebase.controller

import ai.sovereignrag.commons.tenant.KnowledgeBaseStatus
import ai.sovereignrag.commons.user.UserGateway
import ai.sovereignrag.tenant.knowledgebase.command.CreateKnowledgeBaseCommand
import ai.sovereignrag.tenant.knowledgebase.dto.KnowledgeBaseDto
import ai.sovereignrag.tenant.knowledgebase.dto.KnowledgeBaseSummaryDto
import ai.sovereignrag.tenant.knowledgebase.query.GetKnowledgeBaseQuery
import ai.sovereignrag.tenant.knowledgebase.query.GetKnowledgeBasesQuery
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/knowledge-bases")
class KnowledgeBaseController(
    private val pipeline: Pipeline,
    private val userGateway: UserGateway
) {

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT_SUPER_ADMIN', 'ROLE_MERCHANT_ADMIN')")
    fun createKnowledgeBase(@RequestBody request: CreateKnowledgeBaseRequest): ResponseEntity<CreateKnowledgeBaseResponse> {
        val userId = userGateway.getLoggedInUserId()?.toString()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CreateKnowledgeBaseResponse(success = false, message = "error.unauthorized"))

        val organizationId = getOrganizationIdFromToken()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CreateKnowledgeBaseResponse(success = false, message = "error.organization_not_found"))

        log.info { "Creating knowledge base: ${request.name} for organization: $organizationId by user: $userId" }

        val command = CreateKnowledgeBaseCommand(
            name = request.name,
            organizationId = organizationId,
            createdByUserId = userId,
            description = request.description
        )

        val result = pipeline.send(command)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateKnowledgeBaseResponse(
                success = true,
                message = "knowledge_base.created_successfully",
                knowledgeBase = result.knowledgeBase,
                clientId = result.clientId,
                clientSecret = result.clientSecret
            )
        )
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT_SUPER_ADMIN', 'ROLE_MERCHANT_ADMIN', 'ROLE_MERCHANT_USER')")
    fun listKnowledgeBases(
        @RequestParam(required = false) status: KnowledgeBaseStatus?
    ): ResponseEntity<List<KnowledgeBaseSummaryDto>> {
        val organizationId = getOrganizationIdFromToken()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(emptyList())

        log.debug { "Listing knowledge bases for organization: $organizationId with status filter: $status" }

        val query = GetKnowledgeBasesQuery(
            organizationId = organizationId,
            status = status
        )

        val result = pipeline.send(query)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{knowledgeBaseId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT_SUPER_ADMIN', 'ROLE_MERCHANT_ADMIN', 'ROLE_MERCHANT_USER')")
    fun getKnowledgeBase(@PathVariable knowledgeBaseId: String): ResponseEntity<KnowledgeBaseDto> {
        val organizationId = getOrganizationIdFromToken()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        log.debug { "Getting knowledge base: $knowledgeBaseId for organization: $organizationId" }

        val query = GetKnowledgeBaseQuery(
            knowledgeBaseId = knowledgeBaseId,
            organizationId = organizationId
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

data class CreateKnowledgeBaseRequest(
    val name: String,
    val description: String? = null
)

data class CreateKnowledgeBaseResponse(
    val success: Boolean,
    val message: String? = null,
    val knowledgeBase: KnowledgeBaseDto? = null,
    val clientId: String? = null,
    val clientSecret: String? = null
)
