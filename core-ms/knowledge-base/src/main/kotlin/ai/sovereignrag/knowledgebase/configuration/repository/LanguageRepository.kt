package ai.sovereignrag.knowledgebase.configuration.repository

import ai.sovereignrag.knowledgebase.configuration.domain.Language
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LanguageRepository : JpaRepository<Language, String> {
    fun findByEnabledTrueOrderBySortOrder(): List<Language>
}
