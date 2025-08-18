package com.legistrack.ingestion.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * Typed configuration for the data ingestion scheduler.
 * Prefix: app.scheduler.data-ingestion
 */
@ConfigurationProperties(prefix = "app.scheduler.data-ingestion")
@Validated
data class DataIngestionSchedulerProperties(
    /** Whether the scheduled ingestion job is enabled. */
    val enabled: Boolean = true,
    /** Cron expression controlling execution frequency. */
    @field:NotBlank
    val cron: String = "0 0 */6 * * *",
    /** Number of days to look back when ingesting recent documents. */
    @field:Min(1)
    val lookbackDays: Long = 7,
)
