package com.legistrack.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCache
import java.util.concurrent.Callable

class ApiInstrumentedCacheLoadLatencyTest {
    @Test
    fun `should record valueLoader latency`() {
        val registry = SimpleMeterRegistry()
        val underlying = ConcurrentMapCache("demo2")
        val cache = ApiInstrumentedCache(underlying, registry)

        val value = cache.get("lazy", Callable { Thread.sleep(5); "computed" })
        assertThat(value).isEqualTo("computed")

        val timer = registry.find("cache.load.latency").tag("name", "demo2").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(5.0)
    }
}
