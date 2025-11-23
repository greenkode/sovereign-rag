package ai.sovereignrag.identity.commons

enum class RoleEnum(val description: String, val value: String) {

    ROLE_MERCHANT_USER("Viewer Role", "ROLE_MERCHANT_USER"),
    ROLE_MERCHANT_ADMIN("Admin Role", "ROLE_MERCHANT_ADMIN"),
    ROLE_MERCHANT_SUPER_ADMIN("Super Admin Role", "ROLE_MERCHANT_SUPER_ADMIN");

    companion object {
        fun of(value: String): RoleEnum? {
            return values().firstOrNull { it.value == value }
        }
    }
}