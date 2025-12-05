package ai.sovereignrag.commons.exception

import java.util.UUID

abstract class SrServiceException protected constructor(
    message: String,
    val parameters: Array<out String> = emptyArray(),
    val responseCode: ResponseCode = ResponseCode.GENERAL_ERROR
) : RuntimeException(message) {

    val code: ExceptionCodeEnum = ExceptionCodeEnum.AN_ERROR_OCCURRED
}

class ProcessServiceException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.GENERAL_ERROR) :
    SrServiceException(message, parameters, responseCode)

class BankingIntegrationException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.GENERAL_ERROR) :
    SrServiceException(message, parameters, responseCode)

class TransactionServiceException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TRANSACTION_FAILED) :
    SrServiceException(message, parameters, responseCode)

class TransactionProcessingException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TRANSACTION_FAILED) :
    SrServiceException(message, parameters, responseCode)

class RecordNotFoundException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.UNABLE_TO_LOCATE_RECORD) :
    SrServiceException(message, parameters, responseCode)

class TransactionRecordNotFoundException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.UNABLE_TO_LOCATE_RECORD) :
    SrServiceException(message, parameters, responseCode)

class DuplicateRecordException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.DUPLICATE_TRANSACTION_REF) :
    SrServiceException(message, parameters, responseCode)

class InvalidRequestException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.INVALID_TRANSACTION) :
    SrServiceException(message, parameters, responseCode)

class PricingServiceException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.PRICING_ERROR) :
    SrServiceException(message, parameters, responseCode)

class AccountServiceException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.INVALID_ACCOUNT) :
    SrServiceException(message, parameters, responseCode)

class IntegrationException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.GENERAL_ERROR) :
    SrServiceException(message, parameters, responseCode)

class BankingServiceException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.GENERAL_ERROR) :
    SrServiceException(message, parameters, responseCode)

class IllegalProcessDataException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.INVALID_TRANSACTION) :
    SrServiceException(message, parameters, responseCode)

class FundingSourceAuthorizationRequiredException(
    val title: String,
    message: String,
    val reference: UUID,
    val pinLength: Int,
    vararg parameters: String,
    responseCode: ResponseCode = ResponseCode.TRANSACTION_NOT_PERMITTED
) :
    SrServiceException(message, parameters, responseCode)

class InvalidCredentialsException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.INVALID_CREDENTIALS) :
    SrServiceException(message, parameters, responseCode)

class TwoFactorSessionInvalidException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TWO_FACTOR_SESSION_INVALID) :
    SrServiceException(message, parameters, responseCode)

class TwoFactorCodeInvalidException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TWO_FACTOR_CODE_INVALID) :
    SrServiceException(message, parameters, responseCode)

class TwoFactorCodeExpiredException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TWO_FACTOR_CODE_EXPIRED) :
    SrServiceException(message, parameters, responseCode)

class TwoFactorMaxAttemptsException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TWO_FACTOR_MAX_ATTEMPTS) :
    SrServiceException(message, parameters, responseCode)

class TwoFactorResendRateLimitException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.TWO_FACTOR_RESEND_RATE_LIMIT) :
    SrServiceException(message, parameters, responseCode)

class TwoFactorAuthenticationRequiredException(
    val sessionId: String,
    message: String,
    vararg parameters: String,
    responseCode: ResponseCode = ResponseCode.TWO_FACTOR_REQUIRED
) :
    SrServiceException(message, parameters, responseCode)

class EmailNotVerifiedException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.EMAIL_NOT_VERIFIED) :
    SrServiceException(message, parameters, responseCode)

class PhoneNumberNotVerifiedException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.PHONE_NOT_VERIFIED) :
    SrServiceException(message, parameters, responseCode)

class OrganizationNotSetupException(message: String, vararg parameters: String, responseCode: ResponseCode = ResponseCode.ORGANIZATION_SETUP_REQUIRED) :
    SrServiceException(message, parameters, responseCode)
