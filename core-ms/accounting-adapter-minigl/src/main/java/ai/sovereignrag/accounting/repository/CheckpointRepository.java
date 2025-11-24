package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.CheckpointEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Checkpoint entities
 */
@Repository
public interface CheckpointRepository extends JpaRepository<CheckpointEntity, CheckpointEntity.CheckpointId> {
    
    /**
     * Find most recent checkpoint for given criteria
     */
    @Query("SELECT c FROM CheckpointEntity c WHERE c.journal = :journal " +
           "AND c.account = :account " +
           "AND (:layers IS NULL OR c.layers = :layers) " +
           "AND (:date IS NULL OR c.date <= :date) " +
           "ORDER BY c.date DESC")
    Optional<CheckpointEntity> findMostRecentCheckpoint(
        @Param("journal") JournalEntity journal,
        @Param("account") GLAccountEntity account,
        @Param("layers") String layers,
        @Param("date") Instant date
    );
    
    /**
     * Find checkpoints to invalidate
     */
    @Query("SELECT c FROM CheckpointEntity c WHERE c.journal = :journal " +
           "AND c.account IN :accounts " +
           "AND (:layers IS NULL OR c.layers = :layers) " +
           "AND c.date >= :startDate " +
           "AND (:endDate IS NULL OR c.date <= :endDate)")
    List<CheckpointEntity> findCheckpointsToInvalidate(
        @Param("journal") JournalEntity journal,
        @Param("accounts") List<GLAccountEntity> accounts,
        @Param("layers") String layers,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
    
    /**
     * Delete checkpoints by criteria
     */
    @Modifying
    @Query("DELETE FROM CheckpointEntity c WHERE c.journal = :journal " +
           "AND c.account IN :accounts " +
           "AND (:layers IS NULL OR c.layers = :layers) " +
           "AND c.date >= :startDate " +
           "AND (:endDate IS NULL OR c.date <= :endDate)")
    void deleteCheckpoints(
        @Param("journal") JournalEntity journal,
        @Param("accounts") List<GLAccountEntity> accounts,
        @Param("layers") String layers,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
}