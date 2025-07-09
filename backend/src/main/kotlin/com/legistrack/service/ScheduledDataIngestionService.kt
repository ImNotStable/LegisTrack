package com.legistrack.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@ConditionalOnProperty(name = ["app.scheduler.data-ingestion.enabled"], havingValue = "true", matchIfMissing = true)
class ScheduledDataIngestionService(
    private val dataIngestionService: DataIngestionService
) {
    
    private val logger = LoggerFactory.getLogger(ScheduledDataIngestionService::class.java)
    
    @Scheduled(cron = "\${app.scheduler.data-ingestion.cron:0 0 * * * ?}") // Every hour by default
    fun performScheduledDataIngestion() {
        logger.info("Starting scheduled data ingestion")
        
        try {
            runBlocking {
                val ingestedCount = dataIngestionService.ingestRecentDocuments(
                    fromDate = LocalDate.now().minusDays(1)
                )
                logger.info("Scheduled data ingestion completed. Documents processed: {}", ingestedCount)
            }
        } catch (e: Exception) {
            logger.error("Error during scheduled data ingestion", e)
        }
    }
    
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // Every 5 minutes, start after 1 minute
    fun healthCheck() {
        logger.debug("Data ingestion service health check - OK")
    }
}
