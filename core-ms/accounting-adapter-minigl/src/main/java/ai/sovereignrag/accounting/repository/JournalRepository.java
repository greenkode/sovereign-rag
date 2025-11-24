package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.JournalEntity;
import ai.sovereignrag.accounting.entity.CompositeAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Journal entities
 */
@Repository
public interface JournalRepository extends JpaRepository<JournalEntity, Long> {
    
    /**
     * Find journal by name
     */
    Optional<JournalEntity> findByName(String name);
    
    /**
     * Find all journals ordered by chart
     */
    @Query("SELECT j FROM JournalEntity j ORDER BY j.chart.id")
    List<JournalEntity> findAllOrderByChart();
    
    /**
     * Find journal by name and chart (for CoaImportService)
     */
    @Query("SELECT j FROM JournalEntity j WHERE j.name = :name AND j.chart = :chart")
    Optional<JournalEntity> findByNameAndChart(@Param("name") String name, @Param("chart") CompositeAccountEntity chart);
}