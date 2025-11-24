package ai.sovereignrag.commons.user.dto

import ai.sovereignrag.commons.kyc.TrustLevel
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class UserKycDetailsDto(
    val publicId: UUID,
    val merchantId: UUID?,
    val trustLevel: TrustLevel,
    val userType: UserType,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val email: String?,
    val phoneNumber: String?,
    val taxIdentificationNumber: String?,
    val address: UserAddress?,
    val dob: LocalDate?,
    val locale: Locale
) : java.io.Serializable