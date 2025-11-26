package ai.sovereignrag.audit.exception

import ai.sovereignrag.audit.config.MessageService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

private val log = KotlinLogging.logger {}

data class ErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null
)

@RestControllerAdvice
class GlobalExceptionHandler(private val messageService: MessageService) {

    @ExceptionHandler(AuditClientException::class)
    fun handleAuditClientException(
        ex: AuditClientException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Client error: ${ex.messageKey}" }

        val message = messageService.getMessage(ex.messageKey, *ex.args)
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(AuditNotFoundException::class)
    fun handleAuditNotFoundException(
        ex: AuditNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Not found: ${ex.messageKey}" }

        val message = messageService.getMessage(ex.messageKey, *ex.args)
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(AuditServerException::class)
    fun handleAuditServerException(
        ex: AuditServerException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(ex) { "Server error: ${ex.messageKey}" }

        val message = messageService.getMessage(ex.messageKey, *ex.args)
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(
        ex: org.springframework.security.authorization.AuthorizationDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Access denied: ${ex.message}" }

        val message = messageService.getMessage("audit.error.access_denied")
        val errorResponse = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(ex) { "Runtime error: ${ex.message}" }

        val message = messageService.getMessage("audit.error.internal")
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(ex) { "Unexpected error: ${ex.message}" }

        val message = messageService.getMessage("audit.error.internal")
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = message,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
