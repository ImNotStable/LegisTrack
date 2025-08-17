package com.legistrack.health

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HealthMetricsConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun healthRegistryCustomizer(): MeterRegistryCustomizer<MeterRegistry> = MeterRegistryCustomizer { registry ->
        // Common tags for all health metrics
        registry.config().commonTags("app", "legistrack", "component", "api-rest")
        log.debug("Applied common tags to MeterRegistry for health metrics")
    }
}
