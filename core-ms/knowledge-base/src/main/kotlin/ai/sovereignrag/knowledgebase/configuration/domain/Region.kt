package ai.sovereignrag.knowledgebase.configuration.domain

import ai.sovereignrag.knowledgebase.configuration.dto.RegionDto
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.io.Serializable
import java.time.Instant

@Entity
data class Region(
    @Id
    val code: String,
    val name: String,
    val continent: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val provider: String = "default",
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) : Serializable {
    fun toDto() = RegionDto(
        code = code,
        name = name,
        continent = continent,
        city = city,
        country = country,
        countryCode = countryCode,
        flag = flag,
        enabled = enabled
    )
}
