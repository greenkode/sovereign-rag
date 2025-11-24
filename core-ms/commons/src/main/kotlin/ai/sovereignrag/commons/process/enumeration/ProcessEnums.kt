package ai.sovereignrag.commons.process.enumeration

import ai.sovereignrag.commons.accounting.TransactionType

enum class RequestContextAttributeName {
    CHANNEL
}

object ProcessStrategyBeanNames {
    const val DEPOSIT_PROCESS_STRATEGY = "DepositProcessStrategy"
    const val LIEN_PROCESS_STRATEGY = "LienProcessStrategy"
    const val ACCOUNT_CREATION_PROCESS_STRATEGY = "AccountCreationProcessStrategy"
    const val TRANSACTION_PROCESS_STRATEGY = "TransactionProcessStrategy"
    const val DEFAULT_PROCESS_STRATEGY = "DefaultProcessStrategy"
    const val FUND_POOL_ACCOUNT_PROCESS_STRATEGY = "FundPoolAccountProcessStrategy"
}

enum class ProcessType(val description: String, val timeInSeconds: Long, val strategyBeanName: String? = null) {

    DEPOSIT("Deposit", -1, ProcessStrategyBeanNames.DEPOSIT_PROCESS_STRATEGY),
    LIEN("Lien", -1, ProcessStrategyBeanNames.LIEN_PROCESS_STRATEGY),
    ACCOUNT_CREATION("Account Creation", 300, ProcessStrategyBeanNames.ACCOUNT_CREATION_PROCESS_STRATEGY),
    ELECTRICITY("Electricity", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    AIRTIME("Airtime", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    DATA("Data", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    TV("Tv", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    EDUCATION("Education", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    INSURANCE("Insurance", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    BETTING("Betting", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    INTERNET("Internet", -1, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    WEBHOOK_CREATION("Webhook Configuration Creation", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_UPDATE("Webhook Configuration Update", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_DELETION("Webhook Configuration Deletion", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    FUND_POOL_ACCOUNT("Fund Pool Account", -1, ProcessStrategyBeanNames.FUND_POOL_ACCOUNT_PROCESS_STRATEGY),
    TRANSACTION_RECONCILIATION("Transaction Reconciliation", -1, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    MERCHANT_USER_INVITATION("Merchant User Invitation", 604800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    PASSWORD_RESET("Password Reset", 1200, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TRANSACTION_EXPORT("Transaction Export", -1, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    AI_CHAT("AI Chat", -1, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY);

    fun mapToTransactionType(): TransactionType {
        return when (this) {
            ELECTRICITY -> TransactionType.ELECTRICITY
            AIRTIME -> TransactionType.AIRTIME
            DATA -> TransactionType.DATA
            TV -> TransactionType.TV
            EDUCATION -> TransactionType.EDUCATION
            INSURANCE -> TransactionType.INSURANCE
            BETTING -> TransactionType.BETTING
            FUND_POOL_ACCOUNT -> TransactionType.FUND_POOL_ACCOUNT
            else -> throw IllegalArgumentException("Invalid transaction type")
        }
    }
}

enum class ProcessRequestType {
    CREATE_NEW_PROCESS,
    AUTH_REQUEST,
    CUSTOMER_INFORMATION_UPDATE,
    INTEGRATION_INFORMATION_UPDATE,
    COMPLETE_PROCESS,
    COMPLETE_TRANSACTION,
    UNLIEN_AMOUNT,
    EXPIRE_PROCESS,
    FAIL_PROCESS,
    CREDIT_CHECK,
    ADD_LOAN_DATA,
    INTERNAL_REQUEST,
    STATUS_CHECK_RETRY,
    MANUAL_RECONCILIATION,
    TRANSACTION_REVERSAL,
    CHAT_USER_MESSAGE,
    CHAT_ASSISTANT_MESSAGE
}

enum class ProcessRequestDataName(description: String) {

    SENDER_ACCOUNT_ID("Sender Account Id"),
    RECIPIENT_ACCOUNT_ID("Recipient Account Id"),
    EXTERNAL_REFERENCE("External Reference"),
    AMOUNT("Amount"),
    CURRENCY("Currency"),
    NARRATION("Narration"),
    REQUEST_DATA("Request Data"),
    ACCOUNT_ADDRESS("Account Address"),
    CUSTOMER_ID("Customer id"),
    ACCOUNT_NAME("Name"),
    ALIAS("Alias"),
    SENDER_ACCOUNT_ADDRESS("Sender Account Number"),
    SENDER_NAME("Sender name"),
    RECIPIENT_ACCOUNT_ADDRESS("Recipient Account Number"),
    ADDRESS_TYPE("Address Type"),
    INTEGRATOR_ID("Integrator Id"),
    ACCOUNT_PUBLIC_ID("Account Public Id"),
    TRANSACTION_TYPE("Transaction Type"),
    PRODUCT_ID("Product ID"),
    INTEGRATOR_REFERENCE("Integrator Reference"),
    INTEGRATOR_METADATA("Integrator Metadata"),
    LENDING_OFFERS_METADATA("Lending Offers Metadata"),
    LENDING_OFFER_ID("Lending Offer Id"),
    CUSTOMER_ACCOUNT_NUMBER("Customer Account Number"),
    CLASS_NAME("Class Name"),
    ADDITIONAL_INFORMATION("Additional Information"),
    LOAN_REFERENCE("Loan Reference"),
    LOAN_ID("Loan ID"),
    TICKET_INDEX("Ticket Index"),
    SOURCE("Source"),
    MERCHANT_ID("Merchant ID"),
    WEBHOOK_URL("Webhook URL"),
    WEBHOOK_CONFIG_ID("Webhook Configuration ID"),
    NOTIFICATION_TYPE("Notification Type"),
    CUSTOMER_DETAILS("Customer Details"),
    IS_LEND("Is Lend"),
    RECONCILIATION_CSV_DATA("CSV data for reconciliation"),
    RECONCILIATION_DESCRIPTION("Reconciliation batch description"),
    USER_IDENTIFIER("User Identifier"),
    AUTHENTICATION_REFERENCE("Authentication Reference"),
    START_DATE("Start Date"),
    END_DATE("End Date"),
    USER_EMAIL("User Email"),
    USER_FIRST_NAME("User First Name"),
    USER_LAST_NAME("User Last Name"),
    USER_CHAT_MESSAGE("User Message"),
    AI_CHAT_MESSAGE("AI Chat Message"),;
}

enum class ProcessStakeholderType {

    ACTOR_USER,
    FOR_USER,
    SENDER_USER,
    RECIPIENT_USER,
    REQUESTER_USER,
    MERCHANT
}

enum class ProcessState(val description: String) {
    PENDING("Awaiting further action"),
    FAILED("Process has failed"),
    COMPLETE("Process completed successfully"),
    EXPIRED("Process expired due to timeout"),
    CANCELLED("Process was cancelled"),
    UNKNOWN("State is unknown"),
    INITIAL("Initial state"),
    REJECTED("Process was rejected")
}

enum class ProcessEvent(val description: String) {
    AUTH_SUCCEEDED("Authentication completed successfully"),
    REMOTE_PAYMENT_COMPLETED("Remote payment processing completed"),
    REMOTE_PAYMENT_RESULT("Remote payment result received"),
    ADDED_LOAN_DATA("Loan data added to process"),
    PROCESS_EXPIRED("Process expired due to timeout"),
    PROCESS_FAILED("Process failed with error"),
    PROCESS_COMPLETED("Process completed successfully"),
    LIEN_AMOUNT("Amount held/reserved"),
    UNLIEN_AMOUNT("Amount released from hold"),
    REVERSE_PENDING_FUNDS("Pending funds reversed"),
    REVERSE_TRANSACTION("Completed transaction reversed"),
    PENDING_TRANSACTION_STATUS_VERIFIED("Transaction status verified"),
    PROCESS_CREATED("Process created"),
    CREDIT_RATING_OFFERS_RECEIVED("Credit rating offers received"),
    STATUS_CHECK_FAILED("Status Check failed"),
    MANUAL_RECONCILIATION_CONFIRMED("Manual Reconciliation Confirmed"),
    REVERSAL_REQUESTED("Transaction reversal requested"),
    RECONCILIATION_ITEM_PROCESSED("Reconciliation item processed"),
    CHAT_MESSAGE_RECEIVED("Chat message received"),
}

enum class ProcessHeader {
    PROCESS_ID,
    PROCESS,
    PROCESS_REQUEST_ID
}