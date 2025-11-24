package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA Repository for Currency entities
 */
@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, String> {
    
    /**
     * Get all currency codes
     */
    @Query("SELECT c.id FROM CurrencyEntity c")
    List<String> findAllCurrencyCodes();
    
    /**
     * Find currency by name
     */
    Optional<CurrencyEntity> findByName(String name);

    Set<CurrencyEntity> findAllByNameIn(Set<String> currencyCodes);
}