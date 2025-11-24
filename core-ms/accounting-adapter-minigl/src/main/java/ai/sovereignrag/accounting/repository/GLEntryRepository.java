package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.GLEntryEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA Repository for GLEntryEntity entities
 */
@Repository
public interface GLEntryRepository extends JpaRepository<GLEntryEntity, Long> {
    
    /**
     * Find entries by account and journal
     */
    @Query("SELECT e FROM GLEntryEntity e WHERE e.account = :account " +
           "AND e.transaction.journal = :journal ORDER BY e.transaction.postDate")
    List<GLEntryEntity> findByAccountAndJournal(
        @Param("account") GLAccountEntity account,
        @Param("journal") JournalEntity journal
    );
    
    /**
     * Find entries by account and layer
     */
    @Query("SELECT e FROM GLEntryEntity e WHERE e.account = :account " +
           "AND e.layer IN :layers ORDER BY e.transaction.postDate")
    List<GLEntryEntity> findByAccountAndLayerIn(
        @Param("account") GLAccountEntity account,
        @Param("layers") List<Short> layers
    );
    
    /**
     * Find entries for account detail with date range
     */
    @Query("SELECT e FROM GLEntryEntity e WHERE e.account = :account " +
           "AND e.transaction.journal = :journal " +
           "AND e.layer IN :layers " +
           "AND (:start IS NULL OR e.transaction.postDate >= :start) " +
           "AND (:end IS NULL OR e.transaction.postDate <= :end) " +
           "ORDER BY e.transaction.postDate ASC, e.transaction.timestamp ASC, e.id ASC")
    Page<GLEntryEntity> findAccountDetailAscending(
        @Param("account") GLAccountEntity account,
        @Param("journal") JournalEntity journal,
        @Param("layers") List<Short> layers,
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );
    
    /**
     * Find entries for account detail with date range (descending order)
     */
    @Query("SELECT e FROM GLEntryEntity e WHERE e.account = :account " +
           "AND e.transaction.journal = :journal " +
           "AND e.layer IN :layers " +
           "AND (:start IS NULL OR e.transaction.postDate >= :start) " +
           "AND (:end IS NULL OR e.transaction.postDate <= :end) " +
           "ORDER BY e.transaction.postDate DESC, e.transaction.timestamp DESC, e.id DESC")
    Page<GLEntryEntity> findAccountDetailDescending(
        @Param("account") GLAccountEntity account,
        @Param("journal") JournalEntity journal,
        @Param("layers") List<Short> layers,
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );
    
    /**
     * Calculate balance for account up to a specific date
     */
    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN e.credit = false THEN e.amount ELSE -e.amount END), 0) " +
           "FROM GLEntryEntity e WHERE e.account = :account " +
           "AND e.transaction.journal = :journal " +
           "AND e.layer IN :layers " +
           "AND e.transaction.postDate < :date " +
           "AND (:maxId = 0 OR e.id <= :maxId)")
    BigDecimal calculateBalanceBeforeDate(
        @Param("account") GLAccountEntity account,
        @Param("journal") JournalEntity journal,
        @Param("layers") List<Short> layers,
        @Param("date") Instant date,
        @Param("maxId") Long maxId
    );
    
    /**
     * Count entries for account up to a specific date
     */
    @Query("SELECT COUNT(e) FROM GLEntryEntity e WHERE e.account = :account " +
           "AND e.transaction.journal = :journal " +
           "AND e.layer IN :layers " +
           "AND e.transaction.postDate < :date " +
           "AND (:maxId = 0 OR e.id <= :maxId)")
    Long countEntriesBeforeDate(
        @Param("account") GLAccountEntity account,
        @Param("journal") JournalEntity journal,
        @Param("layers") List<Short> layers,
        @Param("date") Instant date,
        @Param("maxId") Long maxId
    );
    
    /**
     * Find summarized entries (for summarization process)
     */
    @Query("SELECT e.account, SUM(e.amount) FROM GLEntryEntity e " +
           "WHERE e.transaction.journal = :journal " +
           "AND e.transaction.postDate BETWEEN :start AND :end " +
           "AND e.credit = :isCredit " +
           "AND e.layer = :layer " +
           "GROUP BY e.account")
    List<Object[]> findSummarizedEntries(
        @Param("journal") JournalEntity journal,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("isCredit") boolean isCredit,
        @Param("layer") short layer
    );
    
    /**
     * Get maximum entry ID
     */
    @Query("SELECT COALESCE(MAX(e.id), 0) FROM GLEntryEntity e")
    Long getMaxEntryId();
}