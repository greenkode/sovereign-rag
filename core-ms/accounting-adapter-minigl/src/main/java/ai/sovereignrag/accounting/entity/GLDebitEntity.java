package ai.sovereignrag.accounting.entity;

import jakarta.persistence.*;

/**
 * JPA Entity for GLDebit  
 */
@Entity
@DiscriminatorValue("D")
public class GLDebitEntity extends GLEntryEntity {
    
    public GLDebitEntity() {
        super();
        setCredit(false);
    }
}