package ai.sovereignrag.commons.exception

import ai.sovereignrag.commons.enumeration.TransactionFollowUpAction
import org.springframework.http.HttpStatus

/**
 * Standardized response codes for bill payment transactions.
 * Codes follow a professional numbering scheme with consistent formatting.
 */
enum class ResponseCode(
    val code: String,
    val action: TransactionFollowUpAction,
    val description: String,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
) {

    // Success Codes (2000-2099)
    SUCCESS("2000", TransactionFollowUpAction.COMPLETE, "Transaction completed successfully", HttpStatus.OK),
    APPROVED("2001", TransactionFollowUpAction.COMPLETE, "Transaction approved", HttpStatus.OK),
    PENDING("2002", TransactionFollowUpAction.RE_CHECK, "Transaction is being processed", HttpStatus.ACCEPTED),

    // Client Error Codes (4000-4099)
    INVALID_REQUEST("4000", TransactionFollowUpAction.REVERSE, "Invalid request format", HttpStatus.BAD_REQUEST),
    FORMAT_ERROR("4001", TransactionFollowUpAction.REVERSE, "Request format error", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT("4002", TransactionFollowUpAction.REVERSE, "Invalid transaction amount", HttpStatus.BAD_REQUEST),
    INVALID_ACCOUNT("4003", TransactionFollowUpAction.REVERSE, "Invalid account number", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION("4004", TransactionFollowUpAction.REVERSE, "Invalid transaction type", HttpStatus.BAD_REQUEST),
    DUPLICATE_TRANSACTION_REF("4005", TransactionFollowUpAction.REVERSE, "Duplicate transaction reference", HttpStatus.CONFLICT),
    TRANSACTION_NOT_PERMITTED("4006", TransactionFollowUpAction.REVERSE, "Transaction not permitted", HttpStatus.FORBIDDEN),
    INSUFFICIENT_FUNDS("4007", TransactionFollowUpAction.REVERSE, "Insufficient account balance", HttpStatus.PAYMENT_REQUIRED),
    TRANSACTION_LIMIT_EXCEEDED("4008", TransactionFollowUpAction.REVERSE, "Transaction limit exceeded", HttpStatus.FORBIDDEN),
    AUTHENTICATION_FAILED("4009", TransactionFollowUpAction.RE_CHECK, "Authentication failed", HttpStatus.UNAUTHORIZED),
    AUTHORIZATION_FAILED("4010", TransactionFollowUpAction.RE_CHECK, "Authentication failed", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("4011", TransactionFollowUpAction.RE_CHECK, "Access denied", HttpStatus.FORBIDDEN),

    // Server Error Codes (5000-5099)
    SYSTEM_MALFUNCTION("5000", TransactionFollowUpAction.RE_CHECK, "System malfunction", HttpStatus.INTERNAL_SERVER_ERROR),
    GENERAL_ERROR("5001", TransactionFollowUpAction.RE_CHECK, "General system error", HttpStatus.INTERNAL_SERVER_ERROR),
    TIMEOUT_WAITING("5002", TransactionFollowUpAction.REVERSE, "Transaction timeout", HttpStatus.REQUEST_TIMEOUT),
    TRANSACTION_FAILED("5003", TransactionFollowUpAction.REVERSE, "Transaction processing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANSACTION_NOT_FOUND("5004", TransactionFollowUpAction.REVERSE, "Transaction not found", HttpStatus.NOT_FOUND),
    PRICING_ERROR("5005", TransactionFollowUpAction.REVERSE, "Pricing calculation error", HttpStatus.INTERNAL_SERVER_ERROR),

    // Security and Fraud Codes (6000-6099)
    SECURITY_VIOLATION("6000", TransactionFollowUpAction.RE_CHECK, "Security policy violation", HttpStatus.FORBIDDEN),
    SUSPECTED_FRAUD("6001", TransactionFollowUpAction.RE_CHECK, "Suspected fraudulent activity", HttpStatus.FORBIDDEN),
    INCORRECT_PIN("6002", TransactionFollowUpAction.REVERSE, "Incorrect PIN entered", HttpStatus.FORBIDDEN),

    // Identity and Authentication Codes (8000-8099)
    USER_NOT_FOUND("8000", TransactionFollowUpAction.REVERSE, "User not found", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_FOUND("8001", TransactionFollowUpAction.REVERSE, "Organization not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("8002", TransactionFollowUpAction.REVERSE, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    INVALID_INVITATION("8003", TransactionFollowUpAction.REVERSE, "Invalid invitation", HttpStatus.BAD_REQUEST),
    INVITATION_PROCESSING_ERROR("8004", TransactionFollowUpAction.RE_CHECK, "Error processing invitation", HttpStatus.INTERNAL_SERVER_ERROR),
    TWO_FACTOR_SESSION_INVALID("8005", TransactionFollowUpAction.REVERSE, "Invalid 2FA session", HttpStatus.BAD_REQUEST),
    TWO_FACTOR_CODE_INVALID("8006", TransactionFollowUpAction.REVERSE, "Invalid verification code", HttpStatus.BAD_REQUEST),
    TWO_FACTOR_CODE_EXPIRED("8007", TransactionFollowUpAction.REVERSE, "Verification code expired", HttpStatus.BAD_REQUEST),
    TWO_FACTOR_MAX_ATTEMPTS("8008", TransactionFollowUpAction.REVERSE, "Maximum 2FA attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),
    TWO_FACTOR_RESEND_RATE_LIMIT("8009", TransactionFollowUpAction.REVERSE, "2FA resend rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    TWO_FACTOR_REQUIRED("8010", TransactionFollowUpAction.RE_CHECK, "Two-factor authentication required", HttpStatus.PRECONDITION_REQUIRED),
    EMAIL_NOT_VERIFIED("8011", TransactionFollowUpAction.REVERSE, "Email not verified", HttpStatus.FORBIDDEN),
    PHONE_NOT_VERIFIED("8012", TransactionFollowUpAction.REVERSE, "Phone number not verified", HttpStatus.FORBIDDEN),
    ORGANIZATION_SETUP_REQUIRED("8013", TransactionFollowUpAction.RE_CHECK, "Organization setup required", HttpStatus.PRECONDITION_REQUIRED),
    RECORD_NOT_FOUND("8014", TransactionFollowUpAction.REVERSE, "Record not found", HttpStatus.NOT_FOUND),
    CLIENT_ERROR("8015", TransactionFollowUpAction.REVERSE, "Client error", HttpStatus.BAD_REQUEST),
    SERVER_ERROR("8016", TransactionFollowUpAction.RE_CHECK, "Server error", HttpStatus.INTERNAL_SERVER_ERROR),

    // Integration and External Service Codes (7000-7099)
    EXTERNAL_SERVICE_ERROR("7000", TransactionFollowUpAction.RE_CHECK, "External service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    RESPONSE_RECEIVED_TOO_LATE("7001", TransactionFollowUpAction.RE_CHECK, "Response timeout from external service", HttpStatus.GATEWAY_TIMEOUT),
    DO_NOT_HONOUR("7002", TransactionFollowUpAction.REVERSE, "Transaction declined by external service", HttpStatus.PAYMENT_REQUIRED),

    // Unknown and Special Cases (9000-9099)
    UNKNOWN("9000", TransactionFollowUpAction.RE_CHECK, "Unknown transaction status", HttpStatus.INTERNAL_SERVER_ERROR),
    REQUEST_PROCESSING("9001", TransactionFollowUpAction.RE_CHECK, "Request is being processed", HttpStatus.PROCESSING),
    UNABLE_TO_LOCATE_RECORD("9002", TransactionFollowUpAction.REVERSE, "Unable to locate transaction record", HttpStatus.NOT_FOUND);

    companion object {

        /**
         * Find a ResponseCode by its string code value.
         * Returns UNKNOWN if no matching code is found.
         */
        fun fromCode(code: String): ResponseCode {
            return entries.firstOrNull { it.code == code } ?: UNKNOWN
        }

        /**
         * Legacy method for backward compatibility.
         * @deprecated Use fromCode() instead
         */
        @Deprecated("Use fromCode() instead", ReplaceWith("fromCode(value)"))
        fun getCodeForValue(value: String): ResponseCode {
            return fromCode(value)
        }
    }
}