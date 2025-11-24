package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.io.Serializable;

/**
 * JPA Entity for AccountLock
 * Journal level lock.
 */
@Entity
@Table(name = "acctlock")
public class AccountLockEntity {
    
    @EmbeddedId
    private AccountLockId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal", nullable = false, foreignKey = @ForeignKey(name = "FKAccountLockJournal"))
    @MapsId("journal")
    private JournalEntity journal;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "FKAccountLockAccount"))
    @MapsId("account")
    private GLAccountEntity account;
    
    // Constructors
    public AccountLockEntity() {
        super();
    }
    
    public AccountLockEntity(JournalEntity journal, GLAccountEntity account) {
        this.id = new AccountLockId(journal.getId(), account.getId());
        this.journal = journal;
        this.account = account;
    }
    
    // Getters and Setters
    public AccountLockId getId() {
        return id;
    }
    
    public void setId(AccountLockId id) {
        this.id = id;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountLockEntity)) return false;
        AccountLockEntity that = (AccountLockEntity) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    /**
     * Composite ID class for AccountLock
     */
    @Embeddable
    public static class AccountLockId implements Serializable {
        @Column(name = "journal")
        private Long journal;
        
        @Column(name = "account")
        private Long account;
        
        public AccountLockId() {}
        
        public AccountLockId(Long journal, Long account) {
            this.journal = journal;
            this.account = account;
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
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AccountLockId)) return false;
            AccountLockId that = (AccountLockId) o;
            return journal != null && journal.equals(that.journal) &&
                   account != null && account.equals(that.account);
        }
        
        @Override
        public int hashCode() {
            int result = journal != null ? journal.hashCode() : 0;
            result = 31 * result + (account != null ? account.hashCode() : 0);
            return result;
        }
    }
}