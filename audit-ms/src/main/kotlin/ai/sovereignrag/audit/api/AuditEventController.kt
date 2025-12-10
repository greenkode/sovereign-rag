package ai.sovereignrag.audit.api

import ai.sovereignrag.audit.api.dto.AuditEventResponse
import ai.sovereignrag.audit.api.dto.CreateAuditEventRequest
import ai.sovereignrag.audit.api.dto.CreateAuditEventResponse
import ai.sovereignrag.audit.domain.model.PageRequest
import ai.sovereignrag.audit.domain.query.GetAuditLogsByMerchantQuery
import ai.sovereignrag.audit.security.IsService
import ai.sovereignrag.audit.service.SecurityContextService
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1/events")
@Tag(name = "Audit Events", description = "Audit event tracking and retrieval")
class AuditEventController(
    private val pipeline: Pipeline,
    private val securityContextService: SecurityContextService
) {

    @IsService
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create audit event",
        description = "Creates a new audit event for tracking system activities. Internal service endpoint only."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Audit event created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request body"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden - requires service:internal scope"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun createAuditEvent(@RequestBody request: CreateAuditEventRequest): CreateAuditEventResponse {
        log.debug { "Creating audit event: ${request.event} for resource ${request.resource}" }
        val result = pipeline.send(request.toCommand())
        return CreateAuditEventResponse(success = result.success, message = result.message)
    }

    @GetMapping
    @Operation(
        summary = "Find audit events",
        description = "Retrieves paginated audit events for the current merchant"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved audit events"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "DESC") sort: Direction,
        @RequestParam(required = false) actorId: String?,
        @RequestParam(required = false) identityType: String?,
        @RequestParam(required = false) eventType: String?
    ): Page<AuditEventResponse> {
        val merchantId = securityContextService.getCurrentMerchantId()
        log.debug { "Finding audit events for merchant $merchantId, page $page" }

        val query = GetAuditLogsByMerchantQuery(
            merchantId = merchantId.toString(),
            page = PageRequest(page, size, sort)
        )

        return pipeline.send(query).map {
            AuditEventResponse(
                it.actorId,
                it.actorName,
                it.merchantId,
                it.identityType,
                it.resource,
                it.event,
                LocalDateTime.ofInstant(it.eventTime, LocaleContextHolder.getTimeZone().toZoneId()),
                LocalDateTime.ofInstant(it.timeRecorded, LocaleContextHolder.getTimeZone().toZoneId()),
                it.payload,
                it.ipAddress
            )
        }
    }
}
