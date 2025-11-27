package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthAuthenticationMethod
import ai.sovereignrag.identity.core.entity.OAuthGrantType
import ai.sovereignrag.identity.core.entity.OAuthScope
import ai.sovereignrag.identity.core.repository.OAuthAuthenticationMethodRepository
import ai.sovereignrag.identity.core.repository.OAuthGrantTypeRepository
import ai.sovereignrag.identity.core.repository.OAuthScopeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OAuthClientConfigService(
    private val scopeRepository: OAuthScopeRepository,
    private val authMethodRepository: OAuthAuthenticationMethodRepository,
    private val grantTypeRepository: OAuthGrantTypeRepository
) {
    fun findOrCreateScope(name: String): OAuthScope =
        scopeRepository.findByName(name) ?: scopeRepository.save(OAuthScope(name))

    fun findOrCreateAuthenticationMethod(name: String): OAuthAuthenticationMethod =
        authMethodRepository.findByName(name) ?: authMethodRepository.save(OAuthAuthenticationMethod(name))

    fun findOrCreateGrantType(name: String): OAuthGrantType =
        grantTypeRepository.findByName(name) ?: grantTypeRepository.save(OAuthGrantType(name))

    fun findOrCreateScopes(names: List<String>): Set<OAuthScope> =
        names.map { findOrCreateScope(it) }.toSet()

    fun findOrCreateAuthenticationMethods(names: List<String>): Set<OAuthAuthenticationMethod> =
        names.map { findOrCreateAuthenticationMethod(it) }.toSet()

    fun findOrCreateGrantTypes(names: List<String>): Set<OAuthGrantType> =
        names.map { findOrCreateGrantType(it) }.toSet()
}
