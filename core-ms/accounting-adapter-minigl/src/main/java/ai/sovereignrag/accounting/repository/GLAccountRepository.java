package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.CompositeAccountEntity;
import ai.sovereignrag.accounting.entity.FinalAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AccountEntity entities
 */
@Repository
public interface GLAccountRepository extends JpaRepository<GLAccountEntity, Long> {
    
    /**
     * Find account by code within a specific chart (root account)
     */
    @Query("SELECT a FROM GLAccountEntity a WHERE a.root.id = :chartId AND a.code = :code")
    Optional<GLAccountEntity> findByRootIdAndCode(@Param("chartId") Long chartId, @Param("code") String code);
    
    /**
     * Find account by code within a specific chart (root account)
     */
    Optional<GLAccountEntity> findByRootAndCode(CompositeAccountEntity root, String code);
    
    /**
     * Find chart (top-level composite account) by code
     */
    @Query("SELECT a FROM CompositeAccountEntity a WHERE a.code = :code AND a.parent IS NULL")
    Optional<CompositeAccountEntity> findChartByCode(@Param("code") String code);
    
    /**
     * Find all charts (top-level composite accounts)
     */
    @Query("SELECT a FROM CompositeAccountEntity a WHERE a.parent IS NULL")
    List<CompositeAccountEntity> findAllCharts();
    
    /**
     * Find children accounts of a parent account
     */
    @Query("SELECT a FROM GLAccountEntity a WHERE a.parent = :parent ORDER BY a.code")
    List<GLAccountEntity> findByParent(@Param("parent") GLAccountEntity parent);
    
    /**
     * Find final account by code within a specific chart
     */
    @Query("SELECT a FROM FinalAccountEntity a WHERE a.root.id = :chartId AND a.code = :code")
    Optional<FinalAccountEntity> findFinalAccountByRootIdAndCode(@Param("chartId") Long chartId, @Param("code") String code);
    
    /**
     * Find all final accounts within a specific chart
     */
    @Query("SELECT a FROM FinalAccountEntity a WHERE a.root.id = :chartId")
    List<FinalAccountEntity> findFinalAccountsByRootId(@Param("chartId") Long chartId);
    
    /**
     * Find composite account by code within a specific chart
     */
    @Query("SELECT a FROM CompositeAccountEntity a WHERE a.root.id = :chartId AND a.code = :code")
    Optional<CompositeAccountEntity> findCompositeAccountByRootIdAndCode(@Param("chartId") Long chartId, @Param("code") String code);
    
    /**
     * Find all accounts within a specific chart
     */
    @Query("SELECT a FROM GLAccountEntity a WHERE a.root.id = :chartId")
    List<GLAccountEntity> findAllAccountsByRootId(@Param("chartId") Long chartId);
    
    /**
     * Find composite children of a parent account
     */
    @Query("SELECT a FROM CompositeAccountEntity a WHERE a.parent = :parent")
    List<CompositeAccountEntity> findCompositeChildrenByParent(@Param("parent") GLAccountEntity parent);
    
    /**
     * Find final children of a parent account
     */
    @Query("SELECT a FROM FinalAccountEntity a WHERE a.parent = :parent")
    List<FinalAccountEntity> findFinalChildrenByParent(@Param("parent") GLAccountEntity parent);
    
    /**
     * Find composite account by code and root (for CoaImportService)
     */
    @Query("SELECT a FROM CompositeAccountEntity a WHERE a.code = :code AND a.root = :root")
    Optional<CompositeAccountEntity> findCompositeAccountByCodeAndRoot(@Param("code") String code, @Param("root") GLAccountEntity root);
    
    /**
     * Find final account by code and root (for CoaImportService)
     */
    @Query("SELECT a FROM FinalAccountEntity a WHERE a.code = :code AND a.root = :root")
    Optional<FinalAccountEntity> findFinalAccountByCodeAndRoot(@Param("code") String code, @Param("root") GLAccountEntity root);
    
    /**
     * Find chart by code and description (for CoaImportService)
     */
    @Query("SELECT a FROM CompositeAccountEntity a WHERE a.code = :code AND a.parent IS NULL AND a.description = :description")
    Optional<CompositeAccountEntity> findChartByCodeAndDescription(@Param("code") String code, @Param("description") String description);
}