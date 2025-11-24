package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * JPA Entity for Currency
 */
@Entity
@Table(name = "currency")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "minigl-currencies")
public class CurrencyEntity {
    
    @Id
    @Column(name = "id", length = 5)
    private String id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "symbol", length = 16, nullable = false)
    private String symbol;
    
    // Constructors
    public CurrencyEntity() {
        super();
    }
    
    public CurrencyEntity(String id, String name, String symbol) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrencyEntity)) return false;
        CurrencyEntity that = (CurrencyEntity) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}