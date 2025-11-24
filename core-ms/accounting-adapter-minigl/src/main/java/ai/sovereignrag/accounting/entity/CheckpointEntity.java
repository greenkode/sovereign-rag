package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity for Checkpoint
 * Journal level account Checkpoint.
 */
@Entity
@Table(name = "checkpoint")
@IdClass(CheckpointEntity.CheckpointId.class)
public class CheckpointEntity {
    
    @Id
    @Column(name = "date")
    private Instant date;
    
    @Id
    @Column(name = "layers", length = 32)
    private String layers;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal", nullable = false, foreignKey = @ForeignKey(name = "FKCheckpointJournal"))
    private JournalEntity journal;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "FKCheckpointAccount"))
    private GLAccountEntity account;
    
    @Column(name = "balance", nullable = false, precision = 14, scale = 2, columnDefinition = "numeric(14,2)")
    private BigDecimal balance;
    
    // Constructors
    public CheckpointEntity() {
        super();
    }
    
    // Getters and Setters
    public Instant getDate() {
        return date;
    }
    
    public void setDate(Instant date) {
        this.date = date;
    }
    
    public String getLayers() {
        return layers;
    }
    
    public void setLayers(String layers) {
        this.layers = layers;
    }
    
    public JournalEntity getJournal() {
        return journal;
    }
    
    public void setJournal(JournalEntity journal) {
        this.journal = journal;
    }
    
    public GLAccountEntity getAccount() {
        return account;
    }
    
    public void setAccount(GLAccountEntity account) {
        this.account = account;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckpointEntity that)) return false;
        return date != null && date.equals(that.date) &&
               layers != null && layers.equals(that.layers) &&
               journal != null && journal.equals(that.journal) &&
               account != null && account.equals(that.account);
    }
    
    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (layers != null ? layers.hashCode() : 0);
        result = 31 * result + (journal != null ? journal.hashCode() : 0);
        result = 31 * result + (account != null ? account.hashCode() : 0);
        return result;
    }
    
    /**
     * Composite ID class for Checkpoint
     */
    public static class CheckpointId implements Serializable {
        private Instant date;
        private String layers;
        private Long journal;
        private Long account;
        
        public CheckpointId() {}
        
        public CheckpointId(Instant date, String layers, Long journal, Long account) {
            this.date = date;
            this.layers = layers;
            this.journal = journal;
            this.account = account;
        }
        
        // Getters and setters
        public Instant getDate() {
            return date;
        }
        
        public void setDate(Instant date) {
            this.date = date;
        }
        
        public String getLayers() {
            return layers;
        }
        
        public void setLayers(String layers) {
            this.layers = layers;
        }
        
        public Long getJournal() {
            return journal;
        }
        
        public void setJournal(Long journal) {
            this.journal = journal;
        }
        
        public Long getAccount() {
            return account;
        }
        
        public void setAccount(Long account) {
            this.account = account;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CheckpointId that)) return false;
            return date != null && date.equals(that.date) &&
                   layers != null && layers.equals(that.layers) &&
                   journal != null && journal.equals(that.journal) &&
                   account != null && account.equals(that.account);
        }
        
        @Override
        public int hashCode() {
            int result = date != null ? date.hashCode() : 0;
            result = 31 * result + (layers != null ? layers.hashCode() : 0);
            result = 31 * result + (journal != null ? journal.hashCode() : 0);
            result = 31 * result + (account != null ? account.hashCode() : 0);
            return result;
        }
    }
}