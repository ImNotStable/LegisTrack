package com.legistrack.ingestion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.legistrack.ingestion.config.DataIngestionSchedulerProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Clock

/**
 * Periodic trigger for data ingestion. Uses coroutine scope to call suspend ingestion
 * without blocking Spring's scheduling thread.
 */
@Component
@ConditionalOnProperty(name = ["app.scheduler.data-ingestion.enabled"], havingValue = "true", matchIfMissing = true)
class ScheduledDataIngestionService(
    private val dataIngestionService: DataIngestionService,
    private val props: DataIngestionSchedulerProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val logger = LoggerFactory.getLogger(ScheduledDataIngestionService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default)

    // Cron expression now driven by configuration (default every hour top of hour unless overridden)
    @Scheduled(cron = "${'$'}{app.scheduler.data-ingestion.cron}")
    fun scheduledIngestion() {
        scope.launch {
            val fromDate = LocalDate.now(clock).minusDays(props.lookbackDays)
            val count = dataIngestionService.ingestRecentDocuments(fromDate)
            logger.info("[ScheduledIngestion] Ingested {} documents (fromDate={})", count, fromDate)
        }
    }
}
