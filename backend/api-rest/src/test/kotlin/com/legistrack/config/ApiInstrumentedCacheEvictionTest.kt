package com.legistrack.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCache

class ApiInstrumentedCacheEvictionTest {
    @Test
    fun `should increment eviction and clear counters and update size`() {
        val registry = SimpleMeterRegistry()
        val underlying = ConcurrentMapCache("demo3")
        val cache = ApiInstrumentedCache(underlying, registry)

        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("c", 3)
        // Trigger misses/hits to ensure size gauge referenced
        cache.get("a")
        cache.get("x")

        val sizeGauge = registry.find("cache.size").tag("name", "demo3").gauge()
        assertThat(sizeGauge).isNotNull
        assertThat(sizeGauge!!.value()).isEqualTo(3.0)

        cache.evict("b")
        assertThat(sizeGauge.value()).isEqualTo(2.0)
        val evictions = registry.find("cache.evictions").tag("name", "demo3").counter()!!.count()
        assertThat(evictions).isEqualTo(1.0)

        cache.clear()
        assertThat(sizeGauge.value()).isEqualTo(0.0)
        val clears = registry.find("cache.clears").tag("name", "demo3").counter()!!.count()
        assertThat(clears).isEqualTo(1.0)
    }
}
