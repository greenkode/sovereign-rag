package ai.sovereignrag.identity.core.country.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "country", schema = "identity")
class Country(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val publicId: UUID = UUID.randomUUID(),

    val name: String,

    @Column(name = "iso2_code")
    val iso2Code: String,

    @Column(name = "iso3_code")
    val iso3Code: String,

    val numericCode: String,

    val dialCode: String,

    val flagUrl: String,

    val region: String = "",

    val subRegion: String = "",

    val enabled: Boolean = false,

    val createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now()
)
