package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

/**
 * JPA Entity for RuleInfo
 */
@Entity
@Table(name = "ruleinfo")
public class RuleInfoEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "clazz")
    private String clazz;
    
    @Column(name = "layers")
    private String layers;
    
    @Column(name = "param")
    private String param;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal", foreignKey = @ForeignKey(name = "FKRuleInfoJournal"))
    private JournalEntity journal;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account", foreignKey = @ForeignKey(name = "FKRuleInfoAccount"))
    private GLAccountEntity account;
    
    // Constructors
    public RuleInfoEntity() {
        super();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getClazz() {
        return clazz;
    }
    
    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
    
    public String getLayers() {
        return layers;
    }
    
    public void setLayers(String layers) {
        this.layers = layers;
    }
    
    public String getParam() {
        return param;
    }
    
    public void setParam(String param) {
        this.param = param;
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
    
    // Utility method to parse layers as array
    public short[] getLayerArray() {
        if (layers == null || layers.trim().isEmpty()) {
            return new short[0];
        }
        String[] parts = layers.split("\\.");
        short[] result = new short[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Short.parseShort(parts[i].trim());
        }
        return result;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleInfoEntity)) return false;
        RuleInfoEntity that = (RuleInfoEntity) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}