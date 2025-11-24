package ai.sovereignrag.accounting.minigl.account.dao

import ai.sovereignrag.accounting.minigl.account.model.LatestBalanceSnapshot
import ai.sovereignrag.accounting.minigl.account.model.LatestBalanceSnapshotId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LatestBalanceSnapshotJpaRepository : JpaRepository<LatestBalanceSnapshot, LatestBalanceSnapshotId> {
    
    fun findByJournalAndAccountAndLayers(journal: Long, account: String, layers: String): LatestBalanceSnapshot?
    
    @Query("SELECT s FROM LatestBalanceSnapshot s WHERE s.journal = :journal AND s.account IN :accounts AND s.layers = :layers")
    fun findByJournalAndAccountInAndLayers(
        @Param("journal") journal: Long,
        @Param("accounts") accounts: List<String>,
        @Param("layers") layers: String
    ): List<LatestBalanceSnapshot>
}