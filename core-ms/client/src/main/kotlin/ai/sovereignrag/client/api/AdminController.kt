package ai.sovereignrag.client.api

import io.github.oshai.kotlinlogging.KotlinLogging
import ai.sovereignrag.client.domain.Escalation
import ai.sovereignrag.client.dto.PageResponse
import ai.sovereignrag.client.dto.UnansweredQueryDto
import ai.sovereignrag.client.service.EscalationService
import ai.sovereignrag.client.service.UnansweredQueryService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * REST API for admin features (unanswered queries, escalations)
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = ["*"])
class AdminController(
    private val unansweredQueryService: UnansweredQueryService,
    private val escalationService: EscalationService
) {

    // ============================================
    // Unanswered Queries
    // ============================================

    @GetMapping("/unanswered-queries")
    fun getUnansweredQueries(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "open") status: String
    ): PageResponse<UnansweredQueryDto> {
        logger.info { "Getting unanswered queries - page: $page, size: $size, status: $status" }
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "occurrenceCount", "lastOccurredAt")
        )
        val pageData = unansweredQueryService.getQueriesPaginated(status, pageable)
        return PageResponse.from(pageData)
    }

    @GetMapping("/unanswered-queries/statistics")
    fun getQueryStatistics(): Map<String, Any> {
        logger.info { "Getting query statistics" }
        return unansweredQueryService.getQueryStatistics()
    }

    @PostMapping("/unanswered-queries/{id}/mark-reviewed")
    fun markQueryAsReviewed(
        @PathVariable id: UUID,
        @RequestBody(required = false) request: MarkReviewedRequest?
    ): ResponseEntity<Map<String, String>> {
        logger.info { "Marking unanswered query as reviewed: $id" }
        unansweredQueryService.markAsReviewed(id, request?.notes)
        return ResponseEntity.ok(mapOf("status" to "reviewed"))
    }

    @DeleteMapping("/unanswered-queries/{id}")
    fun deleteQuery(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        logger.info { "Deleting unanswered query: $id" }
        unansweredQueryService.deleteQuery(id)
        return ResponseEntity.ok(mapOf("status" to "deleted"))
    }

    // ============================================
    // Escalations
    // ============================================

    @GetMapping("/escalations")
    fun getAllEscalations(): List<Escalation> {
        logger.info { "Getting all escalations" }
        return escalationService.getAllEscalations()
    }

    @GetMapping("/escalations/status/{status}")
    fun getEscalationsByStatus(@PathVariable status: String): List<Escalation> {
        logger.info { "Getting escalations with status: $status" }
        return escalationService.getEscalationsByStatus(status)
    }

    @GetMapping("/escalations/statistics")
    fun getEscalationStatistics(): Map<String, Any> {
        logger.info { "Getting escalation statistics" }
        return escalationService.getEscalationStatistics()
    }

    @PostMapping("/escalations/{id}/review")
    fun markEscalationAsReviewed(
        @PathVariable id: UUID,
        @RequestBody request: ReviewEscalationRequest
    ): ResponseEntity<Map<String, String>> {
        logger.info { "Marking escalation $id as reviewed" }

        escalationService.markAsReviewed(
            id = id,
            reviewedBy = request.reviewedBy,
            status = request.status,
            notes = request.notes
        )

        return ResponseEntity.ok(mapOf("status" to "reviewed"))
    }
}

// DTOs
data class MarkReviewedRequest(
    val notes: String?
)

data class ReviewEscalationRequest(
    val reviewedBy: String,
    val status: String,
    val notes: String?
)