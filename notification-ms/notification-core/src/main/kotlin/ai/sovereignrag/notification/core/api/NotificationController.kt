package ai.sovereignrag.notification.core.api

import ai.sovereignrag.commons.api.ApiResponse
import ai.sovereignrag.commons.notification.dto.SendNotificationRequest
import ai.sovereignrag.commons.notification.dto.SendNotificationResponse
import ai.sovereignrag.notification.core.service.NotificationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @PostMapping("/send")
    fun sendNotification(
        @RequestBody request: SendNotificationRequest
    ): ResponseEntity<ApiResponse<SendNotificationResponse>> {
        log.info { "Received notification request: template=${request.templateName}" }

        val response = notificationService.sendNotification(request)

        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
