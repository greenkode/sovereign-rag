package ai.sovereignrag.commons.scheduler

import org.quartz.Job
import java.time.Instant

data class ScheduleJobPayload(
    val reference: String,
    val group: String,
    val description: String,
    val startAt: Instant,
    val jobType: Class<out Job>,
    val data: Map<String, String> = emptyMap(),
    val repeatForever: Boolean = false,
    val repeatIntervalInSeconds: Int? = null,
    val repeatCount: Int = 0,
    val endAt: Instant? = null,
)