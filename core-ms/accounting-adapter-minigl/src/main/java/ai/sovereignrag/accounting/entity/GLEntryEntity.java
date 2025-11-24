package ai.sovereignrag.accounting.entity;

import ai.sovereignrag.accounting.converter.TagsConverter;
import jakarta.persistence.*;
import ai.sovereignrag.accounting.Tags;

import java.math.BigDecimal;

/**
 * JPA Entity for GLEntryEntity
 */
@Entity
@Table(name = "transentry", indexes = {
        @Index(name = "idx_acct", columnList = "account"),
        @Index(name = "idx_txn", columnList = "transaction")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "subclass", discriminatorType = DiscriminatorType.STRING, length = 1)
@DiscriminatorValue(" ")
@NamedEntityGraph(
        name = "GLEntryEntity.withAccount",
        attributeNodes = @NamedAttributeNode("account")
)
public class GLEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "detail", length = 4096)
    private String detail;

    @Column(name = "tags")
    @Convert(converter = TagsConverter.class)
    private Tags tags;

    @Column(name = "credit", nullable = false, columnDefinition = "char(1)")
    @Convert(converter = org.hibernate.type.YesNoConverter.class)
    private boolean credit;

    @Column(name = "layer", nullable = false, columnDefinition = "int")
    private short layer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "FKGLEntryAccount"))
    private FinalAccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction", foreignKey = @ForeignKey(name = "FKGLEntryTransaction"))
    private GLTransactionEntity transaction;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, columnDefinition = "numeric(14,2)")
    private BigDecimal amount;

    @Transient
    private BigDecimal balance; // non-persistent field for view helper

    // Constructors
    public GLEntryEntity() {
        super();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public boolean isCredit() {
        return credit;
    }

    public void setCredit(boolean credit) {
        this.credit = credit;
    }

    public boolean isDebit() {
        return !credit;
    }

    public void setDebit(boolean debit) {
        this.credit = !debit;
    }

    public short getLayer() {
        return layer;
    }

    public void setLayer(short layer) {
        this.layer = layer;
    }

    public FinalAccountEntity getAccount() {
        return account;
    }

    public void setAccount(FinalAccountEntity account) {
        this.account = account;
    }

    public GLTransactionEntity getTransaction() {
        return transaction;
    }

    public void setTransaction(GLTransactionEntity transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    // Utility methods
    public boolean isIncrease() {
        return (isDebit() && account.isDebit()) || (isCredit() && account.isCredit());
    }

    public boolean isDecrease() {
        return !isIncrease();
    }

    public BigDecimal getImpact() {
        return isIncrease() ? amount : amount.negate();
    }

    public boolean hasLayers(short[] layers) {
        for (short l : layers) {
            if (l == getLayer())
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GLEntryEntity)) return false;
        GLEntryEntity that = (GLEntryEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

