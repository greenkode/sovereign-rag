package ai.sovereignrag.commons.user.dto

import ai.sovereignrag.commons.user.UserPropertyName

data class UserProperty(
    val name: UserPropertyName,
    val value: String) : java.io.Serializable