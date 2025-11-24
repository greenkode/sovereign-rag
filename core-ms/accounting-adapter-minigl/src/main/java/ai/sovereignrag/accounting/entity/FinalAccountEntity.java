package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

import java.util.Set;

/**
 * JPA Entity for FinalAccount
 */
@Entity
@DiscriminatorValue("F")
public class FinalAccountEntity extends GLAccountEntity {
    
    @Override
    public Set<GLAccountEntity> getChildren() {
        return null;
    }
    
    @Override
    public void setChildren(Set<GLAccountEntity> children) {
        throw new IllegalArgumentException("Can't setChildren on FinalAccount");
    }
    
    @Override
    public boolean isFinalAccount() {
        return true;
    }
}