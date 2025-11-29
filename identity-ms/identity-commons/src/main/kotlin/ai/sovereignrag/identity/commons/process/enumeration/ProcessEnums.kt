package ai.sovereignrag.identity.commons.process.enumeration

object ProcessStrategyBeanNames {
    const val DEFAULT_PROCESS_STRATEGY = "DefaultProcessStrategy"
}

enum class ProcessType(val description: String, val timeInSeconds: Long, val strategyBeanName: String? = null) {

    WEBHOOK_CREATION("Webhook Configuration Creation", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_UPDATE("Webhook Configuration Update", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_DELETION("Webhook Configuration Deletion", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    MERCHANT_USER_INVITATION("Merchant User Invitation", 604800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    PASSWORD_RESET("Password Reset", 1200, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TWO_FACTOR_AUTH("Two Factor Authentication", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    EMAIL_VERIFICATION("Email Verification", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    USER_REGISTRATION("User Registration", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    AVATAR_GENERATION("Avatar Generation Session", 1800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY);
}

enum class ProcessRequestType {
    CREATE_NEW_PROCESS,
    COMPLETE_PROCESS,
    FAIL_PROCESS,
    RESEND_AUTHENTICATION,
    AVATAR_PROMPT,
    AVATAR_REFINEMENT,
}

enum class ProcessRequestDataName(description: String) {

    MERCHANT_ID("Merchant ID"),
    USER_IDENTIFIER("User Identifier"),
    AUTHENTICATION_REFERENCE("Authentication Reference"),
    DEVICE_FINGERPRINT("Device Fingerprint"),
    USER_EMAIL("User Email"),
    ORGANIZATION_ID("Organization ID"),
    VERIFICATION_TOKEN("Verification Token"),
    AVATAR_PROMPT("Avatar Generation Prompt"),
    AVATAR_REFINED_PROMPT("Avatar Refined Prompt"),
    AVATAR_IMAGE_KEY("Avatar Image S3 Key");
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
    INITIAL("Initial state"),
}

enum class ProcessEvent(val description: String) {
    PROCESS_EXPIRED("Process expired due to timeout"),
    PROCESS_FAILED("Process failed with error"),
    PROCESS_COMPLETED("Process completed successfully"),
    PENDING_TRANSACTION_STATUS_VERIFIED("Transaction status verified"),
    PROCESS_CREATED("Process created"),
    AUTH_TOKEN_RESEND("Auth Token Resend"),
}