package ai.sovereignrag.identity.core.country.domain

import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
class Country(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(name = "public_id")
    val publicId: UUID = UUID.randomUUID(),

    val name: String,

    @Column(name = "iso2_code")
    val iso2Code: String,

    @Column(name = "iso3_code")
    val iso3Code: String,

    @Column(name = "numeric_code")
    val numericCode: String,

    @Column(name = "dial_code")
    val dialCode: String,

    @Column(name = "flag_url")
    val flagUrl: String,

    val region: String = "",

    @Column(name = "sub_region")
    val subRegion: String = "",

    val enabled: Boolean = false,
) : AuditableEntity()
