package ai.sovereignrag.commons.user.dto

data class UserExternalId(
    val externalId: String,
    val integratorCode: String,
    val integrator: String
) : java.io.Serializable