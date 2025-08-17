package com.legistrack.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Cache instrumentation wrapping the auto-configured CacheManager (e.g., RedisCacheManager) to emit simple counters:
 * cache.requests / cache.hits / cache.misses (tag: name=<cacheName>).
 * Localized to api-rest module where the executable Spring context lives.
 */
// Internal wrapper renamed to avoid any potential duplicate classpath clashes with legacy root placeholder file.
class ApiInstrumentedCache(private val delegate: Cache, private val registry: MeterRegistry) : Cache {
	private val nameTag = delegate.name
	private val hits = registry.counter("cache.hits", "name", nameTag)
	private val misses = registry.counter("cache.misses", "name", nameTag)
	private val requests = registry.counter("cache.requests", "name", nameTag)
	private val loadLatency: Timer = Timer.builder("cache.load.latency").tag("name", nameTag).register(registry)
	private val evictions = registry.counter("cache.evictions", "name", nameTag)
	private val clears = registry.counter("cache.clears", "name", nameTag)

	init {
		// Ratio gauges (-1 when no requests yet to avoid NaN and signal uninitialized state)
		registry.gauge("cache.hit.ratio", listOf(Tag.of("name", nameTag)), this) { it.hitRatio() }
		registry.gauge("cache.miss.ratio", listOf(Tag.of("name", nameTag)), this) { it.missRatio() }
		// Approximate size gauge (-1 when size cannot be determined)
		registry.gauge("cache.size", listOf(Tag.of("name", nameTag)), this) { it.sizeEstimate() }
	}

	private fun hitRatio(): Double {
		val req = requests.count()
		return if (req == 0.0) -1.0 else hits.count() / req
	}

	private fun missRatio(): Double {
		val req = requests.count()
		return if (req == 0.0) -1.0 else misses.count() / req
	}

	override fun getName(): String = delegate.name
	override fun getNativeCache(): Any = delegate.nativeCache

	override fun get(key: Any): Cache.ValueWrapper? {
		requests.increment()
		val v = delegate.get(key)
		if (v == null) misses.increment() else hits.increment()
		return v
	}

	@Suppress("UNCHECKED_CAST", "INAPPLICABLE_OVERRIDE") // Matching Spring's platform type (T may be nullable); suppression avoids future strict nullability mismatch
	override fun <T> get(key: Any, type: Class<T>): T? {
		// Can't know if value will be null until delegate returns
		requests.increment()
		val v = delegate.get(key, type)
		if (v == null) misses.increment() else hits.increment()
		return v
	}


	override fun <T : Any?> get(key: Any, valueLoader: java.util.concurrent.Callable<T>): T? {
		requests.increment()
		val sample = Timer.start()
		val v = try {
			delegate.get(key, valueLoader)
		} finally {
			sample.stop(loadLatency)
		}
		if (v == null) misses.increment() else hits.increment()
		return v
	}

	override fun put(key: Any, value: Any?) = delegate.put(key, value)
	override fun evict(key: Any) {
		delegate.evict(key)
		evictions.increment()
	}
	@Deprecated("Deprecated in Cache interface")
	override fun evictIfPresent(key: Any) = delegate.evictIfPresent(key)
	override fun clear() {
		delegate.clear()
		clears.increment()
	}
	override fun invalidate() = delegate.invalidate()

	private fun sizeEstimate(): Double = when (val native = delegate.nativeCache) {
		is MutableMap<*, *> -> native.size.toDouble()
		else -> -1.0
	}
}

@Configuration
class ApiCacheMetricsConfig {
	@Bean
	@Primary
	fun instrumentedCacheManager(delegate: CacheManager, registry: MeterRegistry): CacheManager = object : CacheManager {
		override fun getCacheNames(): MutableCollection<String> = delegate.cacheNames
		override fun getCache(name: String): Cache? = delegate.getCache(name)?.let { ApiInstrumentedCache(it, registry) }
	}
}
