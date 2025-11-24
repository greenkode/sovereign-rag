package ai.sovereignrag.accounting.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity for Journal
 */
@Entity
@Table(name = "journal")
@NamedEntityGraph(
    name = "Journal.withLayers",
    attributeNodes = @NamedAttributeNode("layers")
)
public class JournalEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name", length = 32, nullable = false, unique = true)
    private String name;
    
    @Column(name = "start")
    private LocalDate start;
    
    @Column(name = "end_")
    private LocalDate end;
    
    @Column(name = "closed")
    private boolean closed;
    
    @Column(name = "lockdate")
    private LocalDate lockDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart", nullable = false, foreignKey = @ForeignKey(name = "FKJournalChart"))
    private GLAccountEntity chart;
    
    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<LayerEntity> layers = new HashSet<>();
    
    // Constructors
    public JournalEntity() {
        super();
    }
    
    // Getters and Setters
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
    
    public LocalDate getStart() {
        return start;
    }
    
    public void setStart(LocalDate start) {
        this.start = start;
    }
    
    public LocalDate getEnd() {
        return end;
    }
    
    public void setEnd(LocalDate end) {
        this.end = end;
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
    public LocalDate getLockDate() {
        return lockDate;
    }
    
    public void setLockDate(LocalDate lockDate) {
        this.lockDate = lockDate;
    }
    
    public GLAccountEntity getChart() {
        return chart;
    }
    
    public void setChart(GLAccountEntity chart) {
        this.chart = chart;
    }
    
    public Set<LayerEntity> getLayers() {
        return layers;
    }
    
    public void setLayers(Set<LayerEntity> layers) {
        this.layers = layers;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JournalEntity that)) return false;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}