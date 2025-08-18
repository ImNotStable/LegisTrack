package com.legistrack.health

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import javax.sql.DataSource
import org.springframework.data.redis.core.StringRedisTemplate
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.port.CongressPort
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class ComponentHealth(
    val status: String,
    val latencyMs: Long? = null,
    val message: String? = null,
    // Timestamp of last known non-DOWN state (ms since epoch). Nullable for components that have never succeeded.
    val lastSuccessEpochMs: Long? = null,
)

data class AggregateHealth(
    val success: Boolean,
    val status: String,
    val components: Map<String, ComponentHealth>,
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Service
class HealthCheckService(
    private val dataSource: DataSource,
    private val jdbcTemplate: JdbcTemplate,
    private val aiModelPort: AiModelPort,
    private val congressPort: CongressPort,
    private val redisTemplate: StringRedisTemplate? = null,
    private val meterRegistry: MeterRegistry? = null
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val registeredStatusGauges = mutableSetOf<String>()

    suspend fun health(): AggregateHealth = coroutineScope {
        // Run independent component checks concurrently to minimize endpoint latency.
        val dbDeferred = async { "database" to runComponent { jdbcTemplate.queryForObject("SELECT 1", Int::class.java); "UP" } }
        val cacheDeferred = async {
            "cache" to (if (redisTemplate != null) runComponent {
                val key = "health:ping"
                try {
                    redisTemplate.opsForValue().set(key, "ok")
                    redisTemplate.opsForValue().get(key)
                    "UP"
                } catch (e: Exception) {
                    // Redis unavailable => treat as degraded (non-critical)
                    "DEGRADED"
                }
            } else ComponentHealth(status = "UNKNOWN", message = "Redis not configured"))
        }
        val ollamaDeferred = async {
            "ollama" to runComponent {
                val ready = runCatching { aiModelPort.isServiceReady() }.getOrDefault(false)
                if (!ready) return@runComponent "DOWN"
                val modelAvailable = runCatching { aiModelPort.isModelAvailable() }.getOrDefault(false)
                when {
                    ready && modelAvailable -> "UP"
                    ready && !modelAvailable -> "DEGRADED" // service up but model missing
                    else -> "DOWN"
                }
            }
        }
        val congressDeferred = async {
            "congressApi" to runComponent {
                if (runCatching { congressPort.ping() }.getOrDefault(false)) "UP" else "DOWN"
            }
        }

        val components = listOf(dbDeferred, cacheDeferred, ollamaDeferred, congressDeferred)
            .awaitAll()
            .toMap()

        // Critical components: if any are DOWN -> overall DOWN
        val criticalComponents = setOf("database", "ollama")
        val criticalDown = components.filter { (name, _) -> name in criticalComponents }
            .values.any { it.status == "DOWN" }

        val anyNonCriticalDown = components.filter { (name, _) -> name !in criticalComponents }
            .values.any { it.status == "DOWN" }

        val anyDegradedOrUnknown = components.values.any { it.status == "DEGRADED" || it.status == "UNKNOWN" }

        val overallStatus = when {
            criticalDown -> "DOWN"
            anyNonCriticalDown || anyDegradedOrUnknown -> "DEGRADED"
            else -> "UP"
        }

        val aggregate = AggregateHealth(
            success = !criticalDown,
            status = overallStatus,
            components = components.toSortedMap(),
            correlationId = MDC.get("correlationId")
        )
    recordMetrics(aggregate)
    aggregate
    }

    private suspend fun runComponent(block: suspend () -> String): ComponentHealth {
        var status = "DOWN"
        var msg: String? = null
        var latency: Long? = null
        var lastSuccess: Long? = null
        try {
            val elapsed = measureTimeMillis { status = block() }
            latency = elapsed
        } catch (e: Exception) {
            msg = e.message ?: e::class.simpleName
            log.debug("Health component failure: ${e.message}", e)
        }
        if (status != "DOWN") {
            lastSuccess = System.currentTimeMillis()
        }
        return ComponentHealth(status = status, latencyMs = latency, message = msg, lastSuccessEpochMs = lastSuccess)
    }

    private fun recordMetrics(aggregate: AggregateHealth) {
        val registry = meterRegistry ?: return
        val baseName = "health.component"
        aggregate.components.forEach { (name, comp) ->
            val statusValue = when (comp.status) {
                "UP" -> 1.0
                "DOWN" -> 0.0
                "DEGRADED" -> 0.5
                else -> -1.0
            }
            // Register/update status gauge (idempotent registration)
            if (registeredStatusGauges.add(name)) {
                registry.gauge(baseName + ".status", listOf(Tag.of("name", name)), statusValue) { statusValue }
            }
            // Use counter for transitions (increment on DOWN occurrences)
            if (comp.status == "DOWN") registry.counter(baseName + ".down.count", "name", name).increment()
            // Record latency if present
            comp.latencyMs?.let { latency ->
                registry.timer(baseName + ".latency", "name", name).record(latency, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
        registry.counter("health.aggregate.invocations").increment()
        val aggregateNumeric = when (aggregate.status) {
            "UP" -> 1.0
            "DEGRADED" -> 0.5
            "DOWN" -> 0.0
            else -> -1.0
        }
        registry.gauge("health.aggregate.status", listOf(Tag.of("status", aggregate.status)), aggregateNumeric)
        // Gauge of number of critical components down
        val criticalDownCount = aggregate.components.filter { (name, comp) ->
            (name == "database" || name == "ollama") && comp.status == "DOWN"
        }.count().toDouble()
        registry.gauge("health.critical.down.count", criticalDownCount)
    }
}
