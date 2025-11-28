package ai.sovereignrag.identity.core.country.controller

import ai.sovereignrag.identity.core.country.domain.CountryRepository
import ai.sovereignrag.identity.core.country.dto.CountryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/countries")
class CountryController(
    private val countryRepository: CountryRepository
) {

    @GetMapping
    fun getEnabledCountries(): List<CountryResponse> {
        return countryRepository.findAllByEnabledTrueOrderByNameAsc()
            .map { CountryResponse.from(it) }
    }

    @GetMapping("/all")
    fun getAllCountries(): List<CountryResponse> {
        return countryRepository.findAllByOrderByNameAsc()
            .map { CountryResponse.from(it) }
    }
}
