package ai.sovereignrag.commons.user.dto

import java.io.Serializable

data class UserExternalId(
    val externalId: String,
    val integratorCode: String,
    val integrator: String
) : Serializable