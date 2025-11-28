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
@Transactional(readOnly = true)
class OAuthClientConfigService(
    private val scopeRepository: OAuthScopeRepository,
    private val authMethodRepository: OAuthAuthenticationMethodRepository,
    private val grantTypeRepository: OAuthGrantTypeRepository
) {
    fun getScope(name: String): OAuthScope =
        scopeRepository.findByName(name)
            ?: throw IllegalStateException("OAuth scope '$name' not found. Ensure database migrations have run.")

    fun getAuthenticationMethod(name: String): OAuthAuthenticationMethod =
        authMethodRepository.findByName(name)
            ?: throw IllegalStateException("OAuth authentication method '$name' not found. Ensure database migrations have run.")

    fun getGrantType(name: String): OAuthGrantType =
        grantTypeRepository.findByName(name)
            ?: throw IllegalStateException("OAuth grant type '$name' not found. Ensure database migrations have run.")
}
