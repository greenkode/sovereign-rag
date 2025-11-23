package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.service.ClientLockedException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class CustomOAuth2ErrorResponseHandler(
    private val objectMapper: ObjectMapper
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        log.error { "OAuth2 authentication failure: ${exception.message}" }
        
        val errorResponse = when (exception) {
            is OAuth2AuthenticationException -> {
                val error = exception.error
                when (error.errorCode) {
                    "client_locked" -> createLockoutErrorResponse(error)
                    else -> createOAuth2ErrorResponse(error)
                }
            }
            is ClientLockedException -> createClientLockoutErrorResponse(exception)
            else -> createDefaultErrorResponse(exception)
        }
        
        // Write error response
        response.status = errorResponse["status"] as Int
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
    
    private fun createLockoutErrorResponse(error: org.springframework.security.oauth2.core.OAuth2Error): Map<String, Any> {
        // Parse remaining minutes from error description if available
        val remainingMinutes = try {
            val description = error.description ?: ""
            val regex = "Try again in (\\d+) minutes".toRegex()
            regex.find(description)?.groupValues?.get(1)?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
        
        return mapOf(
            "status" to HttpStatus.LOCKED.value(),
            "error" to error.errorCode,
            "error_description" to (error.description ?: "Client is locked due to failed authentication attempts"),
            "remaining_minutes" to remainingMinutes
        )
    }
    
    private fun createClientLockoutErrorResponse(exception: ClientLockedException): Map<String, Any> {
        val now = java.time.Instant.now()
        val remainingSeconds = if (now.isBefore(exception.lockedUntil)) {
            exception.lockedUntil.epochSecond - now.epochSecond
        } else {
            0L
        }
        val remainingMinutes = remainingSeconds / 60
        
        return mapOf(
            "status" to HttpStatus.LOCKED.value(),
            "error" to "client_locked",
            "error_description" to "Client is locked due to ${exception.failedAttempts} failed authentication attempts. Try again in $remainingMinutes minutes.",
            "failed_attempts" to exception.failedAttempts,
            "locked_until" to exception.lockedUntil.toString(),
            "remaining_minutes" to remainingMinutes
        )
    }
    
    private fun createOAuth2ErrorResponse(error: org.springframework.security.oauth2.core.OAuth2Error): Map<String, Any> {
        val status = when (error.errorCode) {
            "invalid_client" -> HttpStatus.UNAUTHORIZED
            "invalid_request" -> HttpStatus.BAD_REQUEST
            "invalid_grant" -> HttpStatus.BAD_REQUEST
            "unauthorized_client" -> HttpStatus.FORBIDDEN
            "unsupported_grant_type" -> HttpStatus.BAD_REQUEST
            "invalid_scope" -> HttpStatus.BAD_REQUEST
            "server_error" -> HttpStatus.INTERNAL_SERVER_ERROR
            else -> HttpStatus.UNAUTHORIZED
        }
        
        return mapOf(
            "status" to status.value(),
            "error" to error.errorCode,
            "error_description" to (error.description ?: "Authentication failed"),
            "error_uri" to (error.uri ?: "")
        )
    }
    
    private fun createDefaultErrorResponse(exception: AuthenticationException): Map<String, Any> {
        return mapOf(
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "error" to "authentication_failed",
            "error_description" to (exception.message ?: "Authentication failed")
        )
    }
}