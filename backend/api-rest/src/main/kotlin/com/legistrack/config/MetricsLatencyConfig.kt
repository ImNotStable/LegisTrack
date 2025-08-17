package com.legistrack.config

import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
open class MetricsLatencyConfig {
    @Bean
    open fun latencyDistributionCustomizer(): MeterFilter = object : MeterFilter {
        override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig {
            return if (id.name == "congress.api.latency") {
                DistributionStatisticConfig.builder()
                    .percentiles(0.5, 0.9, 0.95, 0.99)
                    .serviceLevelObjectives(
                        Duration.ofMillis(100).toNanos().toDouble(),
                        Duration.ofMillis(250).toNanos().toDouble(),
                        Duration.ofMillis(500).toNanos().toDouble(),
                        Duration.ofSeconds(1).toNanos().toDouble(),
                        Duration.ofSeconds(2).toNanos().toDouble()
                    )
                    .build()
                    .merge(config)
            } else config
        }
    }
}
