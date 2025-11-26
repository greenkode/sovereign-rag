package ai.sovereignrag.audit.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
open class AuditClientException(
    val messageKey: String,
    val args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : RuntimeException(messageKey, cause)

@ResponseStatus(HttpStatus.NOT_FOUND)
class AuditNotFoundException(
    messageKey: String = "audit.event.not_found",
    args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : AuditClientException(messageKey, args, cause)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
open class AuditServerException(
    val messageKey: String,
    val args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : RuntimeException(messageKey, cause)
