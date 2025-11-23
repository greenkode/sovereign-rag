package ai.sovereignrag.commons.rest

import ai.sovereignrag.commons.enumeration.ResponseCode
import ai.sovereignrag.commons.process.enumeration.ProcessState
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class ErrorResponse(
    var message: String,
    val timestamp: String = LocalDateTime.now().toString(),
    var responseCode: String,
    val status: ProcessState,
    var reference: String? = null,
    var additionalInfo: Map<String, Any> = HashMap()
) {
    constructor(
        message: String,
        errorCode: ResponseCode,
        reference: String?,
        status: ProcessState,
        entry: Map<String, Any>
    ) : this(message, LocalDateTime.now().toString(), errorCode.code, status, reference, entry)

    constructor(
        errorCode: ResponseCode,
        reference: String?,
        status: ProcessState,
        entry: Map<String, Any>
    ) : this(errorCode.name, LocalDateTime.now().toString(), errorCode.code, status, reference, entry)
}
