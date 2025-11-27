package ai.sovereignrag.commons.user

import ai.sovereignrag.commons.user.dto.AccessTokenDto

interface AccessTokenGateway {

    fun findLatestNonExpiredByIntegratorAndResource(institution: String, resource: String): AccessTokenDto?

    fun save(dto: AccessTokenDto): AccessTokenDto
}