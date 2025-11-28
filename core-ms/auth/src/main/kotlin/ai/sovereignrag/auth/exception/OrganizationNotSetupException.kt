package ai.sovereignrag.auth.exception

class OrganizationNotSetupException(
    val status: String = "ORGANIZATION_SETUP_REQUIRED",
    message: String = "Organization setup is required before accessing this resource"
) : RuntimeException(message)
