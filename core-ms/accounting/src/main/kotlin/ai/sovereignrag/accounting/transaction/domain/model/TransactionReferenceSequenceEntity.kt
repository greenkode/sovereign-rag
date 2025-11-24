package ai.sovereignrag.accounting.transaction.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.time.LocalDate

@Entity
@Table(name = "transaction_reference_sequences")
data class TransactionReferenceSequenceEntity(
    @Id
    @Column(name = "date_key", nullable = false)
    @Temporal(TemporalType.DATE)
    val dateKey: LocalDate,

    @Column(name = "sequence_value", nullable = false)
    val sequenceValue: Int
)
