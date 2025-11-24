package ai.sovereignrag.accounting.repository;

import ai.sovereignrag.accounting.entity.LayerEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LayerRepository extends JpaRepository<LayerEntity, Long> {
    
    @Query("SELECT l FROM LayerEntity l WHERE l.journal = :journal AND l.id = :layerId")
    Optional<LayerEntity> findByJournalAndId(@Param("journal") JournalEntity journal, 
                                            @Param("layerId") Short layerId);
    
    @Query("SELECT l FROM LayerEntity l WHERE l.journal = :journal ORDER BY l.id")
    List<LayerEntity> findByJournal(@Param("journal") JournalEntity journal);
    
    @Query("SELECT l FROM LayerEntity l WHERE l.journal = :journal AND l.name = :name")
    Optional<LayerEntity> findByJournalAndName(@Param("journal") JournalEntity journal, 
                                              @Param("name") String name);
}