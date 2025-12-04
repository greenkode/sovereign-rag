package ai.sovereignrag.identity.core.country.dto

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
)
