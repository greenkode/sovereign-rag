package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.util.Set;
import java.util.TreeSet;

/**
 * JPA Entity for CompositeAccount
 */
@Entity
@DiscriminatorValue("C")
public class CompositeAccountEntity extends GLAccountEntity {
    
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("code")
    private Set<GLAccountEntity> children = new TreeSet<>();
    
    @Override
    public Set<GLAccountEntity> getChildren() {
        if (children == null) {
            children = new TreeSet<>();
        }
        return children;
    }
    
    @Override
    public void setChildren(Set<GLAccountEntity> children) {
        this.children = children;
    }
}