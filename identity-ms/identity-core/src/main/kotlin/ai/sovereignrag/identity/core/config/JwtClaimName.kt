package ai.sovereignrag.identity.core.config

enum class JwtClaimName(val value: String) {
    MERCHANT_ID("merchant_id"),
    ORGANIZATION_ID("organization_id"),
    ENVIRONMENT("environment"),
    TYPE("type"),
    CLIENT_TYPE("client_type"),
    ENVIRONMENT_CONFIG("environment_config"),
    ENVIRONMENT_PREFERENCE("environment_preference"),
    MERCHANT_ENVIRONMENT_MODE("merchant_environment_mode"),
    REALM_ACCESS("realm_access"),
    RESOURCE_ACCESS("resource_access"),
    AUTHORITIES("authorities"),
    VERIFICATION_STATUS("verification_status"),
    EMAIL_VERIFIED("email_verified"),
    PHONE_NUMBER_VERIFIED("phone_number_verified"),
    PHONE_NUMBER("phone_number"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    EMAIL("email"),
    PREFERRED_USERNAME("preferred_username"),
    NAME("name"),
    PICTURE("picture"),
    TRUST_LEVEL("trust_level"),
    ROLES("roles"),
    SETUP_COMPLETED("setup_completed"),
    ORGANIZATION_STATUS("organization_status")
}
