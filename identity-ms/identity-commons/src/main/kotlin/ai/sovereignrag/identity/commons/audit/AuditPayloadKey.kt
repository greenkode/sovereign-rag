package ai.sovereignrag.identity.commons.audit

enum class AuditPayloadKey(val value: String) {
    ACTOR_ID("actor_id"),
    ACTOR_NAME("actor_name"),
    MERCHANT_ID("merchant_id"),
    IDENTITY_TYPE("identity_type"),
    RESOURCE("resource"),
    EVENT("event"),
    EVENT_TIME("event_time"),
    PAYLOAD("payload"),
    USERNAME("user_name"),
    IP_ADDRESS("ip_address"),
    USER_ID("user_id"),
    LOGIN_METHOD("login_method"),
    REASON("reason"),
    FAILED_ATTEMPTS("failed_attempts"),
    LOCKED_UNTIL("locked_until"),
    PROCESS_ID("process_id"),
    SESSION_ID("session_id"),
    CODE("code"),
    TRUSTED_DEVICE_ID("trusted_device_id"),
    OLD_JTI("old_jti"),
    REFERENCE("reference"),
    TOKEN("token")
}
