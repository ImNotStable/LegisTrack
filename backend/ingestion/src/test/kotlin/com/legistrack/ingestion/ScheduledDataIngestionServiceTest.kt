package com.legistrack.ingestion

import com.legistrack.ingestion.config.DataIngestionSchedulerProperties
import io.mockk.mockk
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.ZoneOffset
import java.time.Instant
import java.time.LocalDate

class ScheduledDataIngestionServiceTest {

    private val dataIngestionService: DataIngestionService = mockk(relaxed = true)
    private val props = DataIngestionSchedulerProperties(
        enabled = true,
        cron = "0 0 * * * ?",
        lookbackDays = 10,
    )
    private val fixedInstant = Instant.parse("2025-08-17T12:00:00Z")
    private val fixedClock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val service = ScheduledDataIngestionService(dataIngestionService, props, fixedClock)

    @Test
    fun should_useLookbackDays_when_runningScheduledIngestion() {
        // Given a fixed date (2025-08-17)
    val fixedDate = LocalDate.of(2025, 8, 17)
    val expectedFromDate = fixedDate.minusDays(props.lookbackDays)

    service.scheduledIngestion()

    coVerify { dataIngestionService.ingestRecentDocuments(expectedFromDate) }
    }
}
