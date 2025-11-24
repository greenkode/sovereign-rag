package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.GLTransactionEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for GLTransaction entities
 */
@Repository
public interface GLTransactionRepository extends JpaRepository<GLTransactionEntity, Long> {
    
    /**
     * Find transactions by journal and date range using timestamp
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND t.timestamp BETWEEN :start AND :end ORDER BY t.timestamp")
    List<GLTransactionEntity> findByJournalAndTimestampBetween(
        @Param("journal") JournalEntity journal,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Find transactions by journal and date range using postDate
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND t.postDate BETWEEN :start AND :end ORDER BY t.postDate")
    List<GLTransactionEntity> findByJournalAndPostDateBetween(
        @Param("journal") JournalEntity journal,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Find transactions by journal and specific date using timestamp
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND t.timestamp = :date ORDER BY t.timestamp")
    List<GLTransactionEntity> findByJournalAndTimestamp(
        @Param("journal") JournalEntity journal,
        @Param("date") Instant date
    );
    
    /**
     * Find transactions by journal and specific date using postDate
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND t.postDate = :date ORDER BY t.postDate")
    List<GLTransactionEntity> findByJournalAndPostDate(
        @Param("journal") JournalEntity journal,
        @Param("date") Instant date
    );
    
    /**
     * Find transactions with search string in detail
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND t.detail LIKE %:searchString% ORDER BY t.timestamp")
    List<GLTransactionEntity> findByJournalAndDetailContaining(
        @Param("journal") JournalEntity journal,
        @Param("searchString") String searchString
    );
    
    /**
     * Find transactions with pagination
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND (:start IS NULL OR t.postDate >= :start) " +
           "AND (:end IS NULL OR t.postDate <= :end) " +
           "AND (:searchString IS NULL OR t.detail LIKE %:searchString%) " +
           "ORDER BY t.postDate DESC, t.timestamp DESC")
    Page<GLTransactionEntity> findTransactionsWithFilters(
        @Param("journal") JournalEntity journal,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("searchString") String searchString,
        Pageable pageable
    );
    
    /**
     * Count transactions by journal and filters
     */
    @Query("SELECT COUNT(t) FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND (:start IS NULL OR t.postDate >= :start) " +
           "AND (:end IS NULL OR t.postDate <= :end) " +
           "AND (:searchString IS NULL OR t.detail LIKE %:searchString%)")
    Long countTransactionsWithFilters(
        @Param("journal") JournalEntity journal,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("searchString") String searchString
    );
    
    /**
     * Find transaction IDs with pagination
     */
    @Query("SELECT t.id FROM GLTransactionEntity t WHERE t.journal = :journal " +
           "AND (:start IS NULL OR t.postDate >= :start) " +
           "AND (:end IS NULL OR t.postDate <= :end) " +
           "AND (:searchString IS NULL OR t.detail LIKE %:searchString%) " +
           "ORDER BY t.postDate DESC, t.timestamp DESC")
    Page<Long> findTransactionIdsWithFilters(
        @Param("journal") JournalEntity journal,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("searchString") String searchString,
        Pageable pageable
    );
    
    /**
     * Find transaction by detail/reference (for MiniglTransactionRepository)
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.detail = :reference")
    Optional<GLTransactionEntity> findByDetail(@Param("reference") String reference);
    
    /**
     * Find transaction by reference and type pattern (for MiniglTransactionRepository)
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.detail = :reference AND t.tags LIKE :typePattern")
    Optional<GLTransactionEntity> findByDetailAndTagsContaining(@Param("reference") String reference, @Param("typePattern") String typePattern);
    
    /**
     * Search transactions by detail or tags (for MiniglTransactionRepository)
     */
    @Query("SELECT t FROM GLTransactionEntity t WHERE t.detail LIKE :searchTerm OR t.tags LIKE :searchTerm")
    Page<GLTransactionEntity> searchTransactionsByDetailOrTags(@Param("searchTerm") String searchTerm, Pageable pageable);
}