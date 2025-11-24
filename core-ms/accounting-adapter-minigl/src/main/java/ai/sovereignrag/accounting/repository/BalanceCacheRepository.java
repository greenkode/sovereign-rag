package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.BalanceCacheEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for BalanceCache entities
 */
@Repository
public interface BalanceCacheRepository extends JpaRepository<BalanceCacheEntity, BalanceCacheEntity.BalanceCacheId> {
    
    /**
     * Find balance cache by journal, account and layers
     */
    @Query("SELECT bc FROM BalanceCacheEntity bc WHERE bc.journal = :journal " +
           "AND bc.account = :account " +
           "AND (:layers IS NULL OR bc.layers = :layers) " +
           "ORDER BY bc.ref DESC")
    Optional<BalanceCacheEntity> findByJournalAndAccountAndLayers(
        @Param("journal") JournalEntity journal,
        @Param("account") GLAccountEntity account,
        @Param("layers") String layers
    );
    
    /**
     * Delete balance cache entries
     */
    @Modifying
    @Query("DELETE FROM BalanceCacheEntity bc WHERE bc.journal = :journal " +
           "AND (:account IS NULL OR bc.account = :account) " +
           "AND (:layers IS NULL OR bc.layers = :layers)")
    void deleteByJournalAndAccountAndLayers(
        @Param("journal") JournalEntity journal,
        @Param("account") GLAccountEntity account,
        @Param("layers") String layers
    );
}