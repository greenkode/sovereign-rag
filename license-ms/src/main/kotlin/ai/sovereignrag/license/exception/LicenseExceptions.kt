package ai.sovereignrag.license.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
open class LicenseClientException(
    val messageKey: String,
    val args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : RuntimeException(messageKey, cause)

@ResponseStatus(HttpStatus.NOT_FOUND)
class LicenseNotFoundException(
    messageKey: String = "license.error.key_not_found",
    args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : LicenseClientException(messageKey, args, cause)

@ResponseStatus(HttpStatus.NOT_FOUND)
class ClientNotFoundException(
    messageKey: String = "license.error.client_not_found",
    args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : LicenseClientException(messageKey, args, cause)

@ResponseStatus(HttpStatus.FORBIDDEN)
class LicenseRevokedException(
    messageKey: String = "license.error.revoked",
    args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : LicenseClientException(messageKey, args, cause)

@ResponseStatus(HttpStatus.FORBIDDEN)
class LicenseSuspendedException(
    messageKey: String = "license.error.suspended",
    args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : LicenseClientException(messageKey, args, cause)

@ResponseStatus(HttpStatus.FORBIDDEN)
class LicenseExpiredException(
    messageKey: String = "license.error.expired",
    args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : LicenseClientException(messageKey, args, cause)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
open class LicenseServerException(
    val messageKey: String,
    val args: Array<Any> = emptyArray(),
    cause: Throwable? = null
) : RuntimeException(messageKey, cause)
