package ai.sovereignrag.knowledgebase.configuration.repository

import ai.sovereignrag.knowledgebase.configuration.domain.EmbeddingModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EmbeddingModelRepository : JpaRepository<EmbeddingModel, String> {
    fun findByEnabledTrueOrderBySortOrder(): List<EmbeddingModel>

    @Query("""
        SELECT DISTINCT m FROM EmbeddingModel m
        JOIN m.supportedLanguages sl
        WHERE sl IN :languageCodes AND m.enabled = true
        ORDER BY m.sortOrder
    """)
    fun findByLanguagesSupported(languageCodes: Set<String>): List<EmbeddingModel>

    @Query("""
        SELECT DISTINCT m FROM EmbeddingModel m
        JOIN m.optimizedFor ol
        WHERE ol IN :languageCodes AND m.enabled = true
        ORDER BY m.sortOrder
    """)
    fun findByLanguagesOptimized(languageCodes: Set<String>): List<EmbeddingModel>
}
