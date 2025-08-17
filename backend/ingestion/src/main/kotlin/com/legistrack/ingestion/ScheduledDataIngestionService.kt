package com.legistrack.ingestion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Periodic trigger for data ingestion. Uses coroutine scope to call suspend ingestion
 * without blocking Spring's scheduling thread.
 */
@Component
@ConditionalOnProperty(name = ["app.ingestion.scheduling.enabled"], havingValue = "true", matchIfMissing = true)
class ScheduledDataIngestionService(
    private val dataIngestionService: DataIngestionService,
) {
    private val logger = LoggerFactory.getLogger(ScheduledDataIngestionService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default)

    // Run every 6 hours (cron: top of hour every 6 hours)
    @Scheduled(cron = "0 0 */6 * * *")
    fun scheduledIngestion() {
        scope.launch {
            val fromDate = LocalDate.now().minusDays(7)
            val count = dataIngestionService.ingestRecentDocuments(fromDate)
            logger.info("[ScheduledIngestion] Ingested {} documents (fromDate={})", count, fromDate)
        }
    }
}
