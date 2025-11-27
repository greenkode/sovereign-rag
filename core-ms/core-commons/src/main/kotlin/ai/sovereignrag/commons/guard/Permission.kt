package ai.sovereignrag.commons.guard

/**
 * Permissions that control what tools can be executed
 */
enum class Permission {
    /**
     * Read data from knowledge base, search, retrieve documents
     */
    READ,

    /**
     * Write data, create, update content
     */
    WRITE,

    /**
     * Send communications (email, SMS, notifications)
     */
    COMMUNICATE,

    /**
     * Escalate issues to human support
     */
    ESCALATE,

    /**
     * Administrative operations (modify settings, manage users)
     */
    ADMIN,

    /**
     * Destructive operations (delete data, purge content)
     */
    DESTRUCTIVE;

    companion object {
        /**
         * Default permissions for anonymous users
         */
        fun anonymousPermissions(): Set<Permission> = setOf(READ)

        /**
         * Default permissions for authenticated users
         */
        fun authenticatedPermissions(): Set<Permission> = setOf(
            READ,
            WRITE,
            COMMUNICATE,
            ESCALATE
        )

        /**
         * Default permissions for admin users
         */
        fun adminPermissions(): Set<Permission> = setOf(
            READ,
            WRITE,
            COMMUNICATE,
            ESCALATE,
            ADMIN
        )

        /**
         * All permissions (for super-admin)
         */
        fun allPermissions(): Set<Permission> = values().toSet()
    }
}
