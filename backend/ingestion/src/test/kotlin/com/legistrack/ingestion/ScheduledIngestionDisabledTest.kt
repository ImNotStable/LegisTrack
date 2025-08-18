package com.legistrack.ingestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.context.annotation.Configuration

class ScheduledIngestionDisabledTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TestConfig::class.java))
        .withPropertyValues("app.scheduler.data-ingestion.enabled=false")

    @Configuration
    @EnableScheduling
    open class TestConfig

    @Test
    fun should_notRegisterScheduledDataIngestionService_when_disabled() {
        contextRunner.run { ctx ->
            assertFalse(ctx.containsBean("scheduledDataIngestionService"), "ScheduledDataIngestionService should be absent when disabled")
        }
    }
}
