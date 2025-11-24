package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

/**
 * JPA Entity for GLCredit
 */
@Entity
@DiscriminatorValue("C")
public class GLCreditEntity extends GLEntryEntity {
    
    public GLCreditEntity() {
        super();
        setCredit(true);
    }
}