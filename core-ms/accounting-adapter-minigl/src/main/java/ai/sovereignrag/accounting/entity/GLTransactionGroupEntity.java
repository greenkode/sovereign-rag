package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity for GLTransactionGroup
 */
@Entity
@Table(name = "transgroup", indexes = {
    @Index(name = "transgroupname", columnList = "name")
})
public class GLTransactionGroupEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name", length = 32, unique = true)
    private String name;
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "transgroup_transactions",
        joinColumns = @JoinColumn(name = "transactiongroup", foreignKey = @ForeignKey(name = "FKTransactionTransactionGroup")),
        inverseJoinColumns = @JoinColumn(name = "transaction", foreignKey = @ForeignKey(name = "FKTransactionGroupTransaction"))
    )
    @OrderBy("id")
    private Set<GLTransactionEntity> transactions = new HashSet<>();

    public GLTransactionGroupEntity() {
        super();
    }
    
    public GLTransactionGroupEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Set<GLTransactionEntity> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(Set<GLTransactionEntity> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(GLTransactionEntity transaction) {
        transactions.add(transaction);
    }
    
    public void removeTransaction(GLTransactionEntity transaction) {
        transactions.remove(transaction);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GLTransactionGroupEntity that)) return false;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}