package ai.sovereignrag.process.spi

import ai.sovereignrag.commons.cache.CacheNames
import ai.sovereignrag.commons.property.SystemPropertyScope
import ai.sovereignrag.commons.property.SystemPropertyDto
import ai.sovereignrag.commons.property.SystemPropertyGateway
import ai.sovereignrag.commons.property.SystemPropertyName
import ai.sovereignrag.process.dao.SystemPropertyEntity
import ai.sovereignrag.process.dao.SystemPropertyRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SystemPropertyService(private val systemPropertyRepository: SystemPropertyRepository) : SystemPropertyGateway {

    @Cacheable(cacheNames = [CacheNames.SYSTEM_PROPERTIES], key = "#name.name + '_' + #scope.name", unless = "#result == null")
    override fun findByNameAndScope(name: SystemPropertyName, scope: SystemPropertyScope): SystemPropertyDto? {
        return systemPropertyRepository.findByNameAndScope(name, scope)?.toDomain()?.toDto()
    }

    override fun update(
        name: SystemPropertyName,
        scope: SystemPropertyScope,
        value: String
    ) {
        systemPropertyRepository.findByNameAndScope(name, scope)?.let {
            it.value = value
            systemPropertyRepository.save(it)
        } ?: run {
            systemPropertyRepository.save(SystemPropertyEntity(name, scope, value))
        }
    }
}