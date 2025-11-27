package ai.sovereignrag.commons.property

import java.io.Serializable

data class SystemPropertyDto(val id: Int, val name: SystemPropertyName, val scope: SystemPropertyScope, val value: String) : Serializable