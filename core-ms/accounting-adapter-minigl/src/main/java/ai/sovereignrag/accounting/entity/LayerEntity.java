package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.io.Serializable;

/**
 * JPA Entity for Layer
 * Journal level layer information.
 */
@Entity
@Table(name = "layer")
@IdClass(LayerEntity.LayerId.class)
public class LayerEntity {
    
    @Id
    @Column(name = "id")
    private short id;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal", nullable = false)
    private JournalEntity journal;
    
    @Column(name = "name", length = 80)
    private String name;
    
    // Constructors
    public LayerEntity() {
        super();
    }
    
    // Getters and Setters
    public short getId() {
        return id;
    }
    
    public void setId(short id) {
        this.id = id;
    }
    
    public JournalEntity getJournal() {
        return journal;
    }
    
    public void setJournal(JournalEntity journal) {
        this.journal = journal;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LayerEntity)) return false;
        LayerEntity that = (LayerEntity) o;
        return id == that.id && journal != null && journal.equals(that.journal);
    }
    
    @Override
    public int hashCode() {
        int result = (int) id;
        result = 31 * result + (journal != null ? journal.hashCode() : 0);
        return result;
    }
    
    /**
     * Composite ID class for Layer
     */
    public static class LayerId implements Serializable {
        private short id;
        private Long journal;
        
        public LayerId() {}
        
        public LayerId(short id, Long journal) {
            this.id = id;
            this.journal = journal;
        }
        
        // Getters and setters
        public short getId() {
            return id;
        }
        
        public void setId(short id) {
            this.id = id;
        }
        
        public Long getJournal() {
            return journal;
        }
        
        public void setJournal(Long journal) {
            this.journal = journal;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LayerId)) return false;
            LayerId layerId = (LayerId) o;
            return id == layerId.id && journal != null && journal.equals(layerId.journal);
        }
        
        @Override
        public int hashCode() {
            int result = (int) id;
            result = 31 * result + (journal != null ? journal.hashCode() : 0);
            return result;
        }
    }
}