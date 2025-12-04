package ai.sovereignrag.knowledgebase.configuration.repository

import ai.sovereignrag.knowledgebase.configuration.domain.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository : JpaRepository<Region, String> {
    fun findByEnabledTrueOrderBySortOrder(): List<Region>
    fun findByContinentAndEnabledTrue(continent: String): List<Region>
}
