package ai.sovereignrag.identity.config

import mu.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

private val log = KotlinLogging.logger {}

@RestController
class CustomErrorController : ErrorController {
    
    @RequestMapping("/error")
    @ResponseBody
    fun handleError(request: HttpServletRequest, principal: Principal?): Map<String, Any> {
        val status = request.getAttribute("jakarta.servlet.error.status_code") as? Int
        val exception = request.getAttribute("jakarta.servlet.error.exception") as? Throwable
        val message = request.getAttribute("jakarta.servlet.error.message") as? String
        val requestUri = request.getAttribute("jakarta.servlet.error.request_uri") as? String
        
        log.error { "Error occurred - Status: $status, URI: $requestUri, Message: $message, Exception: ${exception?.message}" }
        log.error { "User principal: ${principal?.name}" }
        
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "status" to (status ?: 500),
            "error" to (message ?: "Unknown error"),
            "path" to (requestUri ?: "/unknown"),
            "user" to (principal?.name ?: "anonymous"),
            "details" to mapOf(
                "exception" to (exception?.javaClass?.simpleName ?: "None"),
                "exceptionMessage" to (exception?.message ?: "None"),
                "requestMethod" to request.method,
                "queryString" to request.queryString,
                "headers" to request.headerNames.toList().associateWith { 
                    request.getHeaders(it).toList() 
                }
            )
        )
    }
}