/*
 * Copyright (c) 2025 LegisTrack
 *
 * Licensed under the MIT License. You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.legistrack.health

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import javax.sql.DataSource
import org.springframework.data.redis.core.StringRedisTemplate
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.port.CongressPort
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
    private val redisTemplate: StringRedisTemplate? = null
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // Metrics removed per instrumentation strip request.

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

}
