package ai.sovereignrag.accounting.transaction.domain.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TransactionReferenceRepository : JpaRepository<TransactionReferenceSequenceEntity, LocalDate> {

    @Query(value = """
        INSERT INTO transaction_reference_sequences (date_key, sequence_value) 
        VALUES (:dateKey, 1)
        ON CONFLICT (date_key) 
        DO UPDATE SET sequence_value = transaction_reference_sequences.sequence_value + 1
        RETURNING sequence_value
    """, nativeQuery = true)
    fun getNextSequenceForDate(@Param("dateKey") dateKey: LocalDate): Int

    @Query("DELETE FROM TransactionReferenceSequenceEntity t WHERE t.dateKey < :dateKey")
    fun deleteSequencesBeforeDate(@Param("dateKey") dateKey: LocalDate)
}