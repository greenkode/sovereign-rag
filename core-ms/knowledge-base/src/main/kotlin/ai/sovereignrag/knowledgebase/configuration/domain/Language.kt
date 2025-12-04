package ai.sovereignrag.knowledgebase.configuration.domain

import ai.sovereignrag.knowledgebase.configuration.dto.LanguageDto
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.io.Serializable
import java.time.Instant

@Entity
data class Language(
    @Id
    val code: String,
    val name: String,
    val nativeName: String,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) : Serializable {
    fun toDto() = LanguageDto(
        code = code,
        name = name,
        nativeName = nativeName,
        enabled = enabled
    )
}
