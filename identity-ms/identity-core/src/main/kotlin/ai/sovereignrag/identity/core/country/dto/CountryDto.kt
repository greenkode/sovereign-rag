package ai.sovereignrag.identity.core.country.dto

import ai.sovereignrag.identity.core.country.domain.Country
import java.util.UUID

data class CountryResponse(
    val id: UUID,
    val name: String,
    val iso2Code: String,
    val iso3Code: String,
    val dialCode: String,
    val flagUrl: String,
    val region: String,
    val subRegion: String
) {
    companion object {
        fun from(country: Country) = CountryResponse(
            id = country.publicId,
            name = country.name,
            iso2Code = country.iso2Code,
            iso3Code = country.iso3Code,
            dialCode = country.dialCode,
            flagUrl = country.flagUrl,
            region = country.region,
            subRegion = country.subRegion
        )
    }
}
