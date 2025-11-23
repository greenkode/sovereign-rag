package ai.sovereignrag.identity.commons.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
open class ClientException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

@ResponseStatus(HttpStatus.NOT_FOUND)
open class NotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
open class ServerException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidInvitationException(
    message: String = "Invalid invitation token or reference",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException(
    message: String = "User not found",
    cause: Throwable? = null
) : NotFoundException(message, cause)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InvitationProcessingException(
    message: String = "Error processing invitation",
    cause: Throwable? = null
) : ServerException(message, cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class TwoFactorSessionInvalidException(
    message: String = "Invalid or expired 2FA session",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class TwoFactorCodeInvalidException(
    message: String = "Invalid verification code",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class TwoFactorCodeExpiredException(
    message: String = "Verification code has expired",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class TwoFactorMaxAttemptsException(
    message: String = "Maximum verification attempts exceeded",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class TwoFactorResendRateLimitException(
    message: String = "Please wait before requesting another verification code",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
class TwoFactorAuthenticationRequiredException(
    val sessionId: String,
    val status: String = "2FA_REQUIRED",
    message: String = "Two-factor authentication required",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.FORBIDDEN)
class EmailNotVerifiedException(
    message: String = "Email not verified. Please verify your email before logging in.",
    cause: Throwable? = null
) : ClientException(message, cause)

@ResponseStatus(HttpStatus.FORBIDDEN)
class PhoneNumberNotVerifiedException(
    message: String = "Phone number not verified. Please verify your phone number before logging in.",
    cause: Throwable? = null
) : ClientException(message, cause)