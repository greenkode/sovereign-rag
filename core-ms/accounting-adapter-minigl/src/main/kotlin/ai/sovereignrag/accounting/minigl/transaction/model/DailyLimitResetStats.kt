package ai.sovereignrag.accounting.minigl.transaction.model

import java.time.LocalDate
import java.time.LocalDateTime

data class DailyLimitResetStats(
    var id: Long? = null,
    var executionDate: LocalDate,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    var accountsReset: Int,
    var executionTimeMs: Int? = null,
    var status: String = "SUCCESS",
    var errorMessage: String? = null,
    var createdAt: LocalDateTime? = null
) {
    // No-arg constructor for Hibernate
    constructor() : this(
        id = null,
        executionDate = LocalDate.now(),
        startTime = LocalDateTime.now(),
        endTime = LocalDateTime.now(),
        accountsReset = 0
    )
    
    companion object {
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_SUCCESS_NO_ACCOUNTS = "SUCCESS_NO_ACCOUNTS"
        const val STATUS_FAILED = "FAILED"
    }
}