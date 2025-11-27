package ai.sovereignrag.commons.user.dto

import java.io.Serializable

data class UserProperty(
    val name: UserPropertyName,
    val value: String) : Serializable