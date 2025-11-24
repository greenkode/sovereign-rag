package ai.sovereignrag.identity.commons.exception

import com.giffing.bucket4j.spring.boot.starter.context.RateLimitException
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

data class TwoFactorErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val sessionId: String,
    val path: String? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ClientException::class)
    fun handleClientException(
        ex: ClientException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Client error: ${ex.message}" }
        
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid request",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        ex: NotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Not found error: ${ex.message}" }

        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Resource not found",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(
        ex: RateLimitException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Rate limit exceeded: ${ex.message}" }

        val errorResponse = ErrorResponse(
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            error = "Too Many Requests",
            message = "Please wait before requesting another verification code",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse)
    }

    @ExceptionHandler(TwoFactorAuthenticationRequiredException::class)
    fun handleTwoFactorAuthenticationRequiredException(
        ex: TwoFactorAuthenticationRequiredException,
        request: WebRequest
    ): ResponseEntity<TwoFactorErrorResponse> {
        log.info { "Two-factor authentication required for session: ${ex.sessionId}" }

        val errorResponse = TwoFactorErrorResponse(
            status = HttpStatus.PRECONDITION_REQUIRED.value(),
            error = ex.status,
            message = ex.message ?: "Two-factor authentication required",
            sessionId = ex.sessionId,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(errorResponse)
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(
        ex: org.springframework.security.authorization.AuthorizationDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn { "Access denied: ${ex.message}" }

        val errorResponse = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = "Access denied",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(ServerException::class)
    fun handleServerException(
        ex: ServerException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(ex) { "Server error: ${ex.message}" }

        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(ex) { "Runtime error: ${ex.message}" }
        
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
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
        
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}