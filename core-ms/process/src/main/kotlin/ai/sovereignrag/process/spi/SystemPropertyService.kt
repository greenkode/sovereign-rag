package ai.sovereignrag.process.spi

import ai.sovereignrag.commons.billpay.SystemPropertyName
import ai.sovereignrag.commons.billpay.SystemPropertyScope
import ai.sovereignrag.commons.cache.CacheNames
import ai.sovereignrag.commons.process.SystemPropertyDto
import ai.sovereignrag.commons.process.SystemPropertyGateway
import ai.sovereignrag.process.domain.SystemPropertyEntity
import ai.sovereignrag.process.domain.SystemPropertyRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SystemPropertyService(private val systemPropertyRepository: SystemPropertyRepository) : SystemPropertyGateway {

//    @Cacheable(cacheNames = [CacheNames.SYSTEM_PROPERTIES], key = "#name.name + '_' + #scope.name", unless = "#result == null")
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