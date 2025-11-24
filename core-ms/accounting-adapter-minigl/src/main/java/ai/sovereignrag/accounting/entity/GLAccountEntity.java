package ai.sovereignrag.accounting.entity;

import ai.sovereignrag.accounting.Tags;
import ai.sovereignrag.accounting.converter.TagsConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.Instant;
import java.util.Set;

/**
 * JPA Entity for AccountEntity.
 * Base class for CompositeAccount and FinalAccount.
 */
@Entity(name = "GLAccountEntity")
@Table(name = "acct")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "minigl-accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "subclass", discriminatorType = DiscriminatorType.STRING, length = 1)
@DiscriminatorValue(" ")
public abstract class GLAccountEntity implements Comparable<GLAccountEntity> {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "code", nullable = false)
    private String code;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "tags")
    @Convert(converter = TagsConverter.class)
    private Tags tags;
    
    @Column(name = "created")
    private Instant created;
    
    @Column(name = "expiration")
    private Instant expiration;
    
    @Column(name = "type", columnDefinition = "smallint")
    private int type;
    
    @Column(name = "currency", length = 5, columnDefinition = "char(5)")
    private String currencyCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent", foreignKey = @ForeignKey(name = "FKAccountParent"))
    private CompositeAccountEntity parent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root", foreignKey = @ForeignKey(name = "FKAccountChart"))
    private CompositeAccountEntity root;
    
    // AccountEntity type constants
    public static final int UNDEFINED = 0;
    public static final int DEBIT = 1;
    public static final int CREDIT = 2;
    
    // Constructors
    public GLAccountEntity() {
        super();
    }
    
    // Abstract methods
    public abstract Set<GLAccountEntity> getChildren();
    public abstract void setChildren(Set<GLAccountEntity> children);
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Tags getTags() {
        return tags;
    }
    
    public void setTags(Tags tags) {
        this.tags = tags;
    }
    
    public Instant getCreated() {
        return created;
    }
    
    public void setCreated(Instant created) {
        this.created = created;
    }
    
    public Instant getExpiration() {
        return expiration;
    }
    
    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public CompositeAccountEntity getParent() {
        return parent;
    }
    
    public void setParent(CompositeAccountEntity parent) {
        this.parent = parent;
    }
    
    public CompositeAccountEntity getRoot() {
        return root;
    }
    
    public void setRoot(CompositeAccountEntity root) {
        this.root = root;
    }
    
    // Utility methods
    public boolean isDebit() {
        return (type & DEBIT) == DEBIT;
    }
    
    public boolean isCredit() {
        return (type & CREDIT) == CREDIT;
    }
    
    public boolean isChart() {
        return (type & (DEBIT | CREDIT)) == UNDEFINED;
    }
    
    public boolean isFinalAccount() {
        return false;
    }
    
    public boolean isCompositeAccount() {
        return !isFinalAccount();
    }
    
    public String getTypeAsString() {
        if (isDebit())
            return "debit";
        else if (isCredit())
            return "credit";
        else
            return null;
    }

    public boolean equalsType (GLAccountEntity other) {
        return (
                getType() & (CREDIT | DEBIT))
                == (other.getType() & (CREDIT | DEBIT));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GLAccountEntity)) return false;
        GLAccountEntity that = (GLAccountEntity) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public int getLevel () {
        return getParent() == null ? 0 : getLevel(0);
    }

    public int getLevel (int level) {
        level++;
        if (getParent() == null)
            return level;
        if (getParent().equals(getRoot()))
            return level;
        else
            return getParent().getLevel(level);
    }
    
    @Override
    public int compareTo(GLAccountEntity other) {
        if (other == null) return 1;
        if (this.code == null && other.code == null) return 0;
        if (this.code == null) return -1;
        if (other.code == null) return 1;
        return this.code.compareTo(other.code);
    }
}

