package ai.sovereignrag.accounting.minigl.journal

import ai.sovereignrag.accounting.entity.CompositeAccountEntity
import ai.sovereignrag.accounting.entity.JournalEntity
import ai.sovereignrag.accounting.repository.JournalRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class JournalService(val journalRepository: JournalRepository) {

    fun getJournal(chart: CompositeAccountEntity): JournalEntity? {
        return journalRepository.findByName("${chart.description}-${ LocalDate.now().year}")
            .orElse(null)
    }
}