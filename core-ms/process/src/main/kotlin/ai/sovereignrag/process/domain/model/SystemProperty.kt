package ai.sovereignrag.process.domain.model

import ai.sovereignrag.commons.property.SystemPropertyDto
import ai.sovereignrag.commons.property.SystemPropertyName
import ai.sovereignrag.commons.property.SystemPropertyScope

data class SystemProperty(val id: Int, val name: SystemPropertyName, val scope: SystemPropertyScope, val value: String) {
    fun toDto(): SystemPropertyDto {
        return SystemPropertyDto(id, name, scope, value)
    }
}