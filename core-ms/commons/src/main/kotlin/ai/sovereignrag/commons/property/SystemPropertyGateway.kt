package ai.sovereignrag.commons.property

interface SystemPropertyGateway {

    fun findByNameAndScope(name: SystemPropertyName, scope: SystemPropertyScope): SystemPropertyDto?

    fun update(name: SystemPropertyName, scope: SystemPropertyScope, value: String)
}