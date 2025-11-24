package ai.sovereignrag.accounting.entity;

import ai.sovereignrag.accounting.Util;
import ai.sovereignrag.accounting.converter.TagsConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jdom2.Element;
import ai.sovereignrag.accounting.Tags;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JPA Entity for GLTransactionEntity
 */
@Entity
@Table(name = "transacc")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "minigl-transactions")
@NamedEntityGraph(
    name = "GLTransactionEntity.withEntries",
    attributeNodes = @NamedAttributeNode("entries")
)
public class GLTransactionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "detail", length = 4096)
    private String detail;

    @Convert(converter = TagsConverter.class)
    private Tags tags;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "postdate")
    private Instant postDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal", nullable = false, foreignKey = @ForeignKey(name = "FKTransactionJournal"))
    private JournalEntity journal;
    
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "posn")
    private List<GLEntryEntity> entries = new ArrayList<>();
    
    // Constructors
    public GLTransactionEntity() {
        super();
    }
    
    public GLTransactionEntity(String detail) {
        super();
        this.detail = detail;
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
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Instant getPostDate() {
        return postDate;
    }
    
    public void setPostDate(Instant postDate) {
        this.postDate = postDate;
    }
    
    public JournalEntity getJournal() {
        return journal;
    }
    
    public void setJournal(JournalEntity journal) {
        this.journal = journal;
    }
    
    public List<GLEntryEntity> getEntries() {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return entries;
    }
    
    public void setEntries(List<GLEntryEntity> entries) {
        this.entries = entries;
    }
    
    // Helper method to add an entry
    public void addEntry(GLEntryEntity entry) {
        getEntries().add(entry);
        entry.setTransaction(this);
    }
    
    // Helper method to remove an entry
    public void removeEntry(GLEntryEntity entry) {
        getEntries().remove(entry);
        entry.setTransaction(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GLTransactionEntity that)) return false;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * Create ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     * @param detail detail
     * @param isCredit true if credit, false if debit
     * @param layer the layer
     */
    public GLEntryEntity createGLEntry (FinalAccountEntity acct, BigDecimal amount, String detail, boolean isCredit, short layer)
    {
        GLEntryEntity entry = isCredit ? new GLCreditEntity() : new GLDebitEntity();
        entry.setAccount (acct);
        entry.setAmount (amount);
        entry.setDetail (detail);
        entry.setCredit (isCredit);
        entry.setTransaction (this);
        entry.setLayer (layer);
        getEntries().add (entry);
        return entry;
    }
    /**
     * Create a debit ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     */
    public GLEntryEntity createDebit (FinalAccountEntity acct, BigDecimal amount)
    {
        return createGLEntry (acct, amount, null, false, (short) 0);
    }
    /**
     * Create a debit ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     * @param detail detail
     */
    public GLEntryEntity createDebit
    (FinalAccountEntity acct, BigDecimal amount, String detail)
    {
        return createGLEntry (acct, amount, detail, false, (short) 0);
    }
    /**
     * Create a debit ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     * @param detail detail
     * @param layer  layer
     */
    public GLEntryEntity createDebit
    (FinalAccountEntity acct, BigDecimal amount, String detail, short layer)
    {
        return createGLEntry (acct, amount, detail, false, layer);
    }

    /**
     * Create a credit ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     */
    public GLEntryEntity createCredit (FinalAccountEntity acct, BigDecimal amount) {
        return createGLEntry (acct, amount, null, true, (short) 0);
    }
    /**
     * Create a credit ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     * @param detail detail
     */
    public GLEntryEntity createCredit
    (FinalAccountEntity acct, BigDecimal amount, String detail)
    {
        return createGLEntry (acct, amount, detail, true, (short) 0);
    }
    /**
     * Create a credit ai.sovereignrag.minigl.GLEntryEntity associated with this ai.sovereignrag.minigl.GLTransactionEntity.
     *
     * Factory helper method.
     *
     * @param acct the account
     * @param amount amount
     * @param layer the layer
     * @param detail detail
     */
    public GLEntryEntity createCredit
    (FinalAccountEntity acct, BigDecimal amount, String detail, short layer)
    {
        return createGLEntry (acct, amount, detail, true, layer);
    }

    /**
     *
     * Create a reverse transaction based on this one
     *
     * @return a reversal transaction
     */
    public GLTransactionEntity createReverse() {
        return createReverse(false);
    }

    /**
     *
     * Create a reverse transaction based on this one
     *
     * @param keepEntryTags if true entries tags are copied to the reversal entries
     * @return a reversal transaction
     */
    public GLTransactionEntity createReverse(boolean keepEntryTags) {
        GLTransactionEntity glt = new GLTransactionEntity (negate(getDetail()));
        glt.setJournal (getJournal());
        for (GLEntryEntity e : getEntries()) {
            GLEntryEntity reversalEntry = glt.createGLEntry(
                    e.getAccount(),
                    negate(e.getAmount()),
                    e.getDetail(),
                    e.isCredit(),
                    e.getLayer()
            );
            if (keepEntryTags) reversalEntry.setTags(e.getTags());
        }
        return glt;
    }

    /**
     *
     * Create a reverse transaction based on this one
     *
     * @param keepEntryTags if true entries tags are copied to the reversal entries
     * @param layers entries with layer <code>layer</code> are selected
     * @return a reversal transaction
     */
    public GLTransactionEntity createReverse(boolean keepEntryTags, short... layers) {
        GLTransactionEntity glt = new GLTransactionEntity ("(" + getDetail() + ")");
        glt.setJournal (getJournal());
        for (GLEntryEntity e : getEntries()) {
            if (ArrayUtils.contains(layers,e.getLayer())) {
                GLEntryEntity reversalEntry = glt.createGLEntry(
                        e.getAccount(),
                        negate(e.getAmount()),
                        e.getDetail(),
                        e.isCredit(),
                        e.getLayer()
                );
                if (keepEntryTags) reversalEntry.setTags(e.getTags());
            }
        }
        return glt;
    }



    /**
     *
     * Create a reverse transaction based on this one
     *
     * @param withTags entries with any tag in <code>withTags</code> are selected (can be null)
     * @param withoutTags entries with any tag in <code>withoutTags</code> are discarded (can be null)
     *
     * @return a reversal transaction
     */
    public GLTransactionEntity createReverse(Tags withTags, Tags withoutTags) {
        return createReverse(withTags, withoutTags, false);
    }

    /**
     *
     * Create a reverse transaction based on this one
     *
     * @param withTags entries with any tag in <code>withTags</code> are selected (can be null)
     * @param withoutTags entries with any tag in <code>withoutTags</code> are discarded (can be null)
     *
     * @param keepEntryTags if true, reverse entries have the same tags as the original ones
     * @return a reversal transaction
     */
    public GLTransactionEntity createReverse(Tags withTags, Tags withoutTags, boolean keepEntryTags) {
        GLTransactionEntity glt = new GLTransactionEntity ("(" + getDetail() + ")");
        glt.setJournal (getJournal());
        for (GLEntryEntity e : getEntries()) {
            Tags tags = e.getTags() != null ? e.getTags() : new Tags();
            if ((withTags == null || tags.containsAll(withTags)) &&
                    (withoutTags == null || !tags.containsAny(withoutTags)))
            {
                GLEntryEntity reversalEntry = glt.createGLEntry(
                        e.getAccount(),
                        negate(e.getAmount()),
                        e.getDetail(),
                        e.isCredit(),
                        e.getLayer()
                );
                if (keepEntryTags) reversalEntry.setTags(e.getTags());
            }
        }
        return glt;
    }

    /**
     * Create a simplified transaction based on this one
     */

    public GLTransactionEntity simplify() {
        GLTransactionEntity glt = new GLTransactionEntity(getDetail());
        for (GLEntryEntity e : getEntries()) {
            if (BigDecimal.ZERO.compareTo(e.getAmount()) != 0) {
                GLEntryEntity redundantEntry = getEntries().stream().filter(entry ->
                        entry.getAccount().equals(e.getAccount()) &&
                                entry.getAmount().compareTo(e.getAmount()) == 0 &&
                                entry.getLayer() == e.getLayer() &&
                                (e.isCredit() ^ entry.isCredit())).findAny().orElse(null);
                if (redundantEntry == null) {
                    glt.createGLEntry(e.getAccount(), e.getAmount(), e.getDetail(), e.isCredit(), e.getLayer());
                    glt.setTags(e.getTags());
                }
            }
        }
        return glt;
    }

    /**
     * Parses a JDOM Element as defined in
     * <a href="http://jpos.org/minigl.dtd">minigl.dtd</a>
     */
    public void fromXML (Element elem) throws ParseException {
        setDetail (elem.getChildTextTrim ("detail"));
        setTags   (new Tags(elem.getChildTextTrim ("tags")));
        setPostDate (
                Util.parseDate (elem.getAttributeValue ("post-date"))
        );
        setTimestamp (
                Util.parseDateTime (elem.getAttributeValue ("date"))
        );
    }
    
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", getId())
                .append("detail", getDetail())
                .toString();
    }

    public boolean hasLayers (short[] layers) {
        for (GLEntryEntity e : getEntries()) {
            if (e.hasLayers (layers))
                return true;
        }
        return false;
    }
    public BigDecimal getDebits (short[] layers) {
        BigDecimal debits = BigDecimal.ZERO;
        for (GLEntryEntity e : getEntries()) {
            if (e.isDebit() && e.hasLayers (layers)) {
                debits = debits.add (e.getAmount());
            }
        }
        return debits;
    }
    public BigDecimal getCredits (short[] layers) {
        BigDecimal credits = BigDecimal.ZERO;
        for (GLEntryEntity e : getEntries()) {
            if (e.isCredit() && e.hasLayers (layers)) {
                credits = credits.add (e.getAmount());
            }
        }
        return credits;
    }

    public BigDecimal getImpact(GLAccountEntity acct, short[] layers) {
        BigDecimal impact = BigDecimal.ZERO;
        for (GLEntryEntity e : getEntries()) {
            if (e.getAccount().equals(acct) && e.hasLayers (layers)) {
                impact = impact.add (e.getImpact());
            }
        }
        return impact;
    }
    private BigDecimal negate (BigDecimal bd) {
        return bd != null ? bd.negate() : null;
    }

    private String negate (String s) {
        if (s == null)
            return s;
        else
            return "(" + s + ")";
    }

    @Override
    public GLTransactionEntity clone() throws CloneNotSupportedException {
        GLTransactionEntity glt = (GLTransactionEntity)super.clone();
        glt.setEntries(null);
        for (GLEntryEntity e : getEntries()) {
            glt.createGLEntry(
                    e.getAccount(),
                    e.getAmount(),
                    e.getDetail(),
                    e.isCredit(),
                    e.getLayer()
            );
        }
        return glt;
    }

    /**
     * Handy method to obtain affected layers considering only entries for which all predicates are true
     * @param predicates Predicates that the inputs have to satisfy, if none, then consider all entries
     * @return layers affected by entries of this transaction.
     */
    @SafeVarargs
    public final Set<Short> getAffectedLayers(Predicate<GLEntryEntity>... predicates) {
        Stream<GLEntryEntity> entryStream = getEntries().stream();
        for (Predicate<GLEntryEntity> p : predicates) entryStream = entryStream.filter(p);
        return entryStream.map(GLEntryEntity::getLayer).collect(Collectors.toSet());
    }

    public Set<Short> getAffectedLayers(Collection<GLAccountEntity> accounts) {
        Objects.requireNonNull(accounts);
        return getAffectedLayers(e -> accounts.contains(e.getAccount()));
    }

    public Set<Short> getAffectedLayers(GLAccountEntity... accounts) {
        return getAffectedLayers(Arrays.asList(accounts));
    }

}