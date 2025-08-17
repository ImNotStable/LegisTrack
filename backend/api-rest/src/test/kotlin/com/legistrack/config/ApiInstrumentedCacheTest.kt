package com.legistrack.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCache

class ApiInstrumentedCacheTest {
    @Test
    fun `should compute hit and miss ratios`() {
        val registry = SimpleMeterRegistry()
        val underlying = ConcurrentMapCache("demo")
        val cache = ApiInstrumentedCache(underlying, registry)

        // No requests yet -> ratios -1
        val hitRatioGauge = registry.find("cache.hit.ratio").tag("name", "demo").gauge()
        val missRatioGauge = registry.find("cache.miss.ratio").tag("name", "demo").gauge()
        assertThat(hitRatioGauge?.value()).isEqualTo(-1.0)
        assertThat(missRatioGauge?.value()).isEqualTo(-1.0)

        // Put an entry then hit it
        cache.put("k1", "v1")
        cache.get("k1") // hit
        cache.get("k2") // miss
        cache.get("k1") // hit
        cache.get("k3") // miss
        cache.get("k1") // hit
        // Totals: requests=5, hits=3, misses=2

        val hits = registry.find("cache.hits").tag("name", "demo").counter()!!.count()
        val misses = registry.find("cache.misses").tag("name", "demo").counter()!!.count()
        val requests = registry.find("cache.requests").tag("name", "demo").counter()!!.count()
        assertThat(hits).isEqualTo(3.0)
        assertThat(misses).isEqualTo(2.0)
        assertThat(requests).isEqualTo(5.0)

    assertThat(hitRatioGauge?.value()).isCloseTo(3.0/5.0, org.assertj.core.data.Offset.offset(0.0000001))
    assertThat(missRatioGauge?.value()).isCloseTo(2.0/5.0, org.assertj.core.data.Offset.offset(0.0000001))
    }
}
