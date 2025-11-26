package ai.sovereignrag.audit.api

import an.awesome.pipelinr.Pipeline
import ai.sovereignrag.audit.api.dto.AuditEventByMerchantRequest
import ai.sovereignrag.audit.api.dto.AuditEventResponse
import ai.sovereignrag.audit.api.dto.CreateAuditEventRequest
import ai.sovereignrag.audit.api.dto.CreateAuditEventResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.OffsetDateTime

@RestController
@RequestMapping("/v1/events")
@Tag(name = "Audit Events", description = "Audit event tracking and retrieval")
class AuditEventController(private val pipeline: Pipeline) {
    @PostMapping("/create")
    @Operation(
        summary = "Create audit event",
        description = "Creates a new audit event for tracking system activities"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Audit event created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request body"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun createAuditEvent(@RequestBody request: CreateAuditEventRequest): ResponseEntity<CreateAuditEventResponse> {
        val result = pipeline.send(request.toCommand())

        return result.takeIf { it.success }
            ?.let {
                ResponseEntity.status(HttpStatus.CREATED).body(
                    CreateAuditEventResponse(success = true, message = it.message)
                )
            }
            ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CreateAuditEventResponse(success = false, message = result.message)
            )
    }

    @PostMapping
    @Operation(
        summary = "Find audit events by merchant",
        description = "Retrieves audit events filtered by merchantId"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved audit events"),
            ApiResponse(responseCode = "400", description = "Invalid request body"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun findAll(@RequestBody body: AuditEventByMerchantRequest): Page<AuditEventResponse> {

        return pipeline.send(body.toDomainQuery()).map {
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
