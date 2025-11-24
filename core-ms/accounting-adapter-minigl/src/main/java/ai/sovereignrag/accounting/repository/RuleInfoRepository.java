package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.JournalEntity;
import ai.sovereignrag.accounting.entity.RuleInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for RuleInfo entities
 */
@Repository
public interface RuleInfoRepository extends JpaRepository<RuleInfoEntity, Long> {
    
    /**
     * Find global rules for a journal (rules not tied to specific accounts)
     */
    @Query("SELECT r FROM RuleInfoEntity r WHERE r.journal = :journal " +
           "AND r.account IS NULL ORDER BY r.id")
    List<RuleInfoEntity> findGlobalRulesForJournal(@Param("journal") JournalEntity journal);
    
    /**
     * Find rules for specific account hierarchies
     */
    @Query("SELECT r FROM RuleInfoEntity r WHERE r.journal = :journal " +
           "AND r.account.id IN :accountIds ORDER BY r.id")
    List<RuleInfoEntity> findRulesForAccountHierarchy(
        @Param("journal") JournalEntity journal,
        @Param("accountIds") List<Long> accountIds
    );
}