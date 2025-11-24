package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * JPA Entity for BalanceCache
 */
@Entity
@Table(name = "balance_cache")
@IdClass(BalanceCacheEntity.BalanceCacheId.class)
public class BalanceCacheEntity {
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal", nullable = false, foreignKey = @ForeignKey(name = "FKBalanceCacheJournal"))
    private JournalEntity journal;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "FKBalanceCacheAccount"))
    private GLAccountEntity account;
    
    @Id
    @Column(name = "layers", length = 32)
    private String layers;
    
    @Column(name = "ref")
    private Long ref;
    
    @Column(name = "balance", nullable = false, precision = 14, scale = 2, columnDefinition = "numeric(14,2)")
    private BigDecimal balance;
    
    // Constructors
    public BalanceCacheEntity() {
        super();
    }
    
    // Getters and Setters
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
    
    public String getLayers() {
        return layers;
    }
    
    public void setLayers(String layers) {
        this.layers = layers;
    }
    
    public Long getRef() {
        return ref;
    }
    
    public void setRef(Long ref) {
        this.ref = ref;
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
        if (!(o instanceof BalanceCacheEntity that)) return false;
        return journal != null && journal.equals(that.journal) &&
               account != null && account.equals(that.account) &&
               layers != null && layers.equals(that.layers);
    }
    
    @Override
    public int hashCode() {
        int result = journal != null ? journal.hashCode() : 0;
        result = 31 * result + (account != null ? account.hashCode() : 0);
        result = 31 * result + (layers != null ? layers.hashCode() : 0);
        return result;
    }
    
    /**
     * Composite ID class for BalanceCache
     */
    public static class BalanceCacheId implements Serializable {
        private Long journal;
        private Long account;
        private String layers;
        
        public BalanceCacheId() {}
        
        public BalanceCacheId(Long journal, Long account, String layers) {
            this.journal = journal;
            this.account = account;
            this.layers = layers;
        }
        
        // Getters and setters
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
        
        public String getLayers() {
            return layers;
        }
        
        public void setLayers(String layers) {
            this.layers = layers;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BalanceCacheId that)) return false;
            return journal != null && journal.equals(that.journal) &&
                   account != null && account.equals(that.account) &&
                   layers != null && layers.equals(that.layers);
        }
        
        @Override
        public int hashCode() {
            int result = journal != null ? journal.hashCode() : 0;
            result = 31 * result + (account != null ? account.hashCode() : 0);
            result = 31 * result + (layers != null ? layers.hashCode() : 0);
            return result;
        }
    }
}