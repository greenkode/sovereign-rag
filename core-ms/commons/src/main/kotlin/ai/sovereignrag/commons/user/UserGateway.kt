package ai.sovereignrag.commons.user

import ai.sovereignrag.commons.user.dto.CreateExternalIdPayload
import ai.sovereignrag.commons.user.dto.CreateUserPayload
import ai.sovereignrag.commons.user.dto.MerchantDetailsDto
import ai.sovereignrag.commons.user.dto.UpdateMerchantEnvironmentResult
import ai.sovereignrag.commons.user.dto.UpdateUserPayload
import ai.sovereignrag.commons.user.dto.UserDetailsDto
import java.util.UUID

interface UserGateway {

    fun getLoggedInUserId(): UUID?

    fun getAuthenticatedUserClaims(): Map<String, Any>

    fun getSystemUserId(): UUID

    fun getSystemMerchantId(): UUID

    fun getLoggedInUserDetails(): UserDetailsDto?

    fun getLoggedInMerchantDetails(): MerchantDetailsDto?

    fun getUserDetailsById(id: UUID): UserDetailsDto?

    fun saveExternalId(createExternalIdPayload: CreateExternalIdPayload)

    fun createUser(createUserPayload: CreateUserPayload)

    fun updateUser(updateUserPayload: UpdateUserPayload)

    fun authorizeAction(userId: UUID, pin: String): Boolean

    fun getMerchantDetailsById(merchantId: UUID): MerchantDetailsDto?

    fun updateMerchantEnvironment(merchantId: String, environmentMode: String): UpdateMerchantEnvironmentResult
}