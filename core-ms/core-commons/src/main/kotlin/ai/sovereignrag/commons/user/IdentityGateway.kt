package ai.sovereignrag.commons.user

import ai.sovereignrag.commons.user.dto.UserKycDetailsDto
import java.util.UUID

interface IdentityGateway {

    fun getUserKycDetails(publicId: UUID) : UserKycDetailsDto?
}