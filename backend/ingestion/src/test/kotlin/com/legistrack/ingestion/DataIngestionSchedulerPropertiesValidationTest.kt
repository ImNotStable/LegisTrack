package com.legistrack.ingestion

import com.legistrack.ingestion.config.DataIngestionSchedulerProperties
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataIngestionSchedulerPropertiesValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun should_fail_when_lookbackDays_invalid() {
        val props = DataIngestionSchedulerProperties(
            enabled = true,
            cron = "0 0 * * * *",
            lookbackDays = 0
        )
        val violations = validator.validate(props)
        assertTrue(violations.any { it.propertyPath.toString() == "lookbackDays" }, "Expected violation on lookbackDays, got $violations")
    }
}
