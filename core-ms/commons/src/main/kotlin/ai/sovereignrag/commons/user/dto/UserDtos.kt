package ai.sovereignrag.commons.user.dto

import ai.sovereignrag.commons.kyc.TrustLevel
import ai.sovereignrag.commons.exception.InvalidRequestException
import com.neovisionaries.i18n.CountryCode
import org.apache.commons.validator.routines.EmailValidator
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class MerchantDetailsDto(
    val id: UUID,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val locale: Locale
) : java.io.Serializable

data class UserDetailsDto(
    val publicId: UUID,
    val merchantId: UUID?,
    val trustLevel: TrustLevel,
    val type: UserType,
    val firstName: String?,
    val lastName: String?,
    val locale: Locale,
    val middleName: String? = null,
    val dateOfBirth: LocalDate? = null,
    val address: UserAddress? = null,
    val email: Email? = null,
    val phoneNumber: PhoneNumber? = null,
    val taxIdentificationNumber: String?,
    val externalIds: MutableSet<UserExternalId> = mutableSetOf(),
    val userProperties: MutableSet<UserProperty> = mutableSetOf()
) : java.io.Serializable

data class Email(val value: String) : java.io.Serializable {
    init {
        if(!EmailValidator.getInstance().isValid(value))
            throw InvalidRequestException("Email address $value is not valid")
    }
}

enum class UserType {
    INDIVIDUAL,
    BUSINESS,
    SYSTEM
}

data class UserAddress(
    val country: CountryCode,
    val state: String,
    val zipCode: String,
    val city: String,
    val street: String,
    val number: String?
) : java.io.Serializable {
    override fun toString() = "$number, $street, $city, $state, $zipCode, $country"
}

