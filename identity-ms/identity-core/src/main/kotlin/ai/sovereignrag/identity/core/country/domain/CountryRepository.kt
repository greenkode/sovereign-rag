package ai.sovereignrag.identity.core.country.domain

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CountryRepository : JpaRepository<Country, Int> {

    @Cacheable(cacheNames = ["countries"], key = "'enabled_countries'")
    fun findAllByEnabledTrueOrderByNameAsc(): List<Country>

    @Cacheable(cacheNames = ["countries"], key = "'all_countries'")
    fun findAllByOrderByNameAsc(): List<Country>

    fun findByIso2Code(iso2Code: String): Country?

    fun findByPublicId(publicId: UUID): Country?
}
