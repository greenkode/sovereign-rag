package ai.sovereignrag.commons.process.enumeration

enum class RequestContextAttributeName {
    CHANNEL
}

object ProcessStrategyBeanNames {
    const val DEFAULT_PROCESS_STRATEGY = "DefaultProcessStrategy"
}

enum class ProcessType(val description: String, val timeInSeconds: Long, val strategyBeanName: String? = null) {
    DEFAULT("Default Process", -1, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY)
}

enum class ProcessRequestType {
    CREATE_NEW_PROCESS,
    CUSTOMER_INFORMATION_UPDATE,
    EXPIRE_PROCESS,
    STATUS_CHECK_RETRY,
    MANUAL_RECONCILIATION,
}

enum class ProcessRequestDataName(description: String) {

    SENDER_ACCOUNT_ID("Sender Account Id"),
    RECIPIENT_ACCOUNT_ID("Recipient Account Id"),
    EXTERNAL_REFERENCE("External Reference"),
    AMOUNT("Amount"),
    CURRENCY("Currency"),
    NARRATION("Narration"),
    ACCOUNT_ADDRESS("Account Address"),
    CUSTOMER_ID("Customer id"),
    SENDER_ACCOUNT_ADDRESS("Sender Account Number"),
    TRANSACTION_TYPE("Transaction Type"),
    RECIPIENT_ACCOUNT_ADDRESS("Recipient Account Number"),
    ADDRESS_TYPE("Address Type"),
    INTEGRATOR_ID("Integrator Id"),
}

enum class ProcessStakeholderType {

    ACTOR_USER,
    FOR_USER,
}

enum class ProcessState(val description: String) {
    PENDING("Awaiting further action"),
    FAILED("Process has failed"),
    COMPLETE("Process completed successfully"),
    EXPIRED("Process expired due to timeout"),
    CANCELLED("Process was cancelled"),
    UNKNOWN("State is unknown"),
    INITIAL("Initial state"),
}

enum class ProcessEvent(val description: String) {
    AUTH_SUCCEEDED("Authentication completed successfully"),
    REMOTE_PAYMENT_COMPLETED("Remote payment processing completed"),
    REMOTE_PAYMENT_RESULT("Remote payment result received"),
    PROCESS_EXPIRED("Process expired due to timeout"),
    PROCESS_FAILED("Process failed with error"),
    PROCESS_COMPLETED("Process completed successfully"),
    REVERSE_PENDING_FUNDS("Pending funds reversed"),
    REVERSE_TRANSACTION("Completed transaction reversed"),
    PENDING_TRANSACTION_STATUS_VERIFIED("Transaction status verified"),
    PROCESS_CREATED("Process created"),
    CREDIT_RATING_OFFERS_RECEIVED("Credit rating offers received"),
    STATUS_CHECK_FAILED("Status Check failed"),
    MANUAL_RECONCILIATION_CONFIRMED("Manual Reconciliation Confirmed"),
}

enum class ProcessHeader {
    PROCESS_ID,
}