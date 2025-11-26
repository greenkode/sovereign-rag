package ai.sovereignrag.commons.exception

import ai.sovereignrag.commons.enumeration.ResponseCode
import ai.sovereignrag.commons.exception.ExceptionCodeEnum.AN_ERROR_OCCURRED
import ai.sovereignrag.commons.i18n.MessageService
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.rest.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(private val messageService: MessageService) {

    private val log = KotlinLogging.logger { }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateRecordException::class)
    fun handleDuplicateRequest(ex: DuplicateRecordException): ErrorResponse {

        log.error(ex) { ex.message!! }

        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SrServiceException::class)
    fun handleInvalidRequestException(ex: SrServiceException): ErrorResponse {

        log.error(ex) { ex.message!! }

        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    @ExceptionHandler(FundingSourceAuthorizationRequiredException::class)
    fun handleFundingSourceAuthorizationRequiredException(ex: FundingSourceAuthorizationRequiredException): Map<String, String> {

        return mapOf(
            "title" to ex.title,
            "message" to ex.message!!,
            "reference" to ex.reference.toString(),
            "pin_length" to ex.pinLength.toString(),
            "response_code" to ex.responseCode.code
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUncaughtException(ex: Exception): ErrorResponse {

        log.error(ex) { ex.message }

        val message = (ex as? SrServiceException)?.message
            ?: messageService.getMessage("commons.error.internal")

        return ErrorResponse(
            message = message,
            errorCode = ResponseCode.GENERAL_ERROR,
            reference = null,
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(ex: AccessDeniedException): ErrorResponse {
        log.error { ex }
        return ErrorResponse(
            message = messageService.getMessage("commons.error.access_denied"),
            errorCode = ResponseCode.ACCESS_DENIED,
            reference = "",
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ErrorResponse {
        (ex as? BadCredentialsException ?: ex as? CredentialsExpiredException)
            ?.let { log.error { it.message } }
            ?: log.error { ex }

        return ErrorResponse(
            message = messageService.getMessage("commons.error.authentication_failed"),
            errorCode = ResponseCode.AUTHENTICATION_FAILED,
            reference = "",
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }


    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RecordNotFoundException::class)
    fun handleRecordNotFoundException(ex: RecordNotFoundException): ErrorResponse {

        log.error(ex) { ex.message!! }

        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ProcessServiceException::class)
    fun handleProcessServiceException(ex: ProcessServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BankingIntegrationException::class)
    fun handleBankingIntegrationException(ex: BankingIntegrationException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(TransactionServiceException::class)
    fun handleTransactionServiceException(ex: TransactionServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(TransactionProcessingException::class)
    fun handleTransactionProcessingException(ex: TransactionProcessingException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(ex: InvalidRequestException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(PricingServiceException::class)
    fun handlePricingServiceException(ex: PricingServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(AccountServiceException::class)
    fun handleAccountServiceException(ex: AccountServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(IntegrationException::class)
    fun handleIntegrationException(ex: IntegrationException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BankingServiceException::class)
    fun handleBankingServiceException(ex: BankingServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalProcessDataException::class)
    fun handleIllegalProcessDataException(ex: IllegalProcessDataException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }
}
