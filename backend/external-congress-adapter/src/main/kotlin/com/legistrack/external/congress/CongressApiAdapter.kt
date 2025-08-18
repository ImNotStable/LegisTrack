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

package com.legistrack.external.congress

import com.legistrack.domain.port.CongressBillDetail
import com.legistrack.domain.port.CongressBillSummary
import com.legistrack.domain.port.CongressBillsPage
import com.legistrack.domain.port.CongressPort
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Internal minimal representations for external API (avoid leaking full external DTO surface)
private data class CongressBillRaw(
    val congress: Int? = null,
    val number: String? = null,
    val type: String? = null,
    val title: String? = null,
    val introducedDate: String? = null,
)
private data class CongressBillsResponse(val bills: List<CongressBillRaw> = emptyList())

// Typed bill detail wrapper (subset) to avoid raw Map parsing
private data class CongressBillDetailWrapper(
    val bill: CongressBillRaw? = null,
)

/**
 * Adapter implementing CongressPort using the public Congress API.
 * Provides caching, retry with backoff + jitter, and sanitized logging.
 */
@Component
open class CongressApiAdapter(
    private val webClient: WebClient,
    private val props: CongressApiProperties,
    // Metrics removed per instrumentation removal requirement
) : CongressPort {
    companion object {
        private val logger = LoggerFactory.getLogger(CongressApiAdapter::class.java)
        private const val RETRY_DELAY_SECONDS = 2L
        // Metric prefix removed
    }
    // Metric fields removed
    private val remainingGaugeHolder = AtomicInteger(-1)
    private val resetGaugeHolder = AtomicInteger(-1)
    private val limitGaugeHolder = AtomicInteger(-1)
    private val last429EpochHolder = AtomicInteger(-1)
    // Circuit breaker state holders
    private val consecutiveFailureHolder = AtomicInteger(0)
    private val breakerStateHolder = AtomicInteger(0) // 0 = closed, 1 = open
    @Volatile private var breakerOpenedAt: Long = -1L
    // Init block metrics removed

    private fun recordResponse(status: Int, headers: org.springframework.http.HttpHeaders) {
    if (status == 429) {
            // Mark timestamp of latest throttling event
            last429EpochHolder.set(Instant.now().epochSecond.toInt())
    }
        headers.getFirst("x-ratelimit-remaining")?.toIntOrNull()?.let { remainingGaugeHolder.set(it) }
        headers.getFirst("x-ratelimit-reset")?.toIntOrNull()?.let { resetGaugeHolder.set(it) }
        headers.getFirst("x-ratelimit-limit")?.toIntOrNull()?.let { limitGaugeHolder.set(it) }
        if (logger.isDebugEnabled) {
            val rem = remainingGaugeHolder.get(); val lim = limitGaugeHolder.get(); val reset = resetGaugeHolder.get()
            if (lim > 0 && rem >= 0) {
                val pct = (rem.toDouble() / lim.toDouble()) * 100.0
                logger.debug("Congress API rate-limit snapshot status={} remaining={} limit={} pct={} resetSec={} last429Epoch={}",
                    status, rem, lim, String.format("%.2f", pct), if (reset >= 0) reset else null, last429EpochHolder.get().takeIf { it >= 0 })
            } else {
                logger.debug("Congress API rate-limit snapshot status={} remaining={} limit={} resetSec={} last429Epoch={}",
                    status, rem, lim, if (reset >= 0) reset else null, last429EpochHolder.get().takeIf { it >= 0 })
            }
        }
    }

    private fun onFailure() {
        val failCount = consecutiveFailureHolder.incrementAndGet()
        if (breakerStateHolder.get() == 0 && failCount >= props.breakerThreshold) {
            breakerStateHolder.set(1)
            breakerOpenedAt = System.nanoTime()
            logger.warn("Congress API circuit breaker opened after $failCount consecutive failures (threshold=${props.breakerThreshold})")
        }
    }

    private fun onSuccess() {
        consecutiveFailureHolder.set(0)
        if (breakerStateHolder.get() == 1) {
            // Close immediately on successful call after open window elapsed
            if (isCooldownElapsed()) {
                breakerStateHolder.set(0)
                logger.info("Congress API circuit breaker closed after cooldown and successful probe")
            }
        }
    }

    private fun isCooldownElapsed(): Boolean {
        if (breakerStateHolder.get() == 0) return true
        if (breakerOpenedAt < 0) return true
        val elapsedSeconds = (System.nanoTime() - breakerOpenedAt) / 1_000_000_000
    return elapsedSeconds >= props.breakerCooldownSeconds
    }

    private inline fun <T> withBreaker(operation: String, fallback: () -> T, block: () -> T): T {
        if (breakerStateHolder.get() == 1 && !isCooldownElapsed()) {
            logger.warn("Short-circuiting $operation due to open circuit breaker")
            return fallback()
        }
        if (breakerStateHolder.get() == 1 && isCooldownElapsed()) {
            // allow a single trial (half-open semantics)
            logger.info("Circuit breaker half-open trial for $operation")
        }
        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }

    private fun buildRetrySpec(attempts: Long): RetryBackoffSpec =
        Retry.backoff(attempts, Duration.ofSeconds(RETRY_DELAY_SECONDS))
            .jitter(0.5)
            .doBeforeRetry { signal ->
                val errorType = signal.failure()?.javaClass?.simpleName ?: "Unknown"
                val attemptIndex = signal.totalRetries().toString()
                // Retry metric removed
            }

    private fun adaptiveRetryAttempts(): Long {
    val configured = props.retryAttempts.coerceAtLeast(0)
        val limit = limitGaugeHolder.get()
        val remaining = remainingGaugeHolder.get()
        if (configured > 0 && limit > 0 && remaining >= 0) {
            val pct = (remaining.toDouble() / limit.toDouble()) * 100.0
            if (pct < props.retryAdaptiveThresholdPercent) { // low remaining budget - suppress retries
                // Adaptive retry suppression metric removed
                return 0
            }
        }
        return configured
    }

    @Cacheable(value = ["congress-bills"], key = "'congress-bills_'+#fromDate.toString()+'_'+#offset+'_'+#limit")
    override suspend fun getRecentBills(fromDate: LocalDate, offset: Int, limit: Int): CongressBillsPage {
    val start = System.nanoTime()
        val dateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val uri = UriComponentsBuilder.fromUriString(props.baseUrl)
            .path("/bill")
            .queryParam("api_key", props.key)
            .queryParam("fromDateTime", "${dateStr}T00:00:00Z")
            .queryParam("sort", "latestAction.actionDate+desc")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
            .queryParam("format", "json")
            .build().toUri()
        logger.debug("Fetching bills from Congress API: {}", sanitizeUri(uri.toString()))
        var outcome = "success"
        val response = runCatching {
            withBreaker("getRecentBills", { CongressBillsResponse() }) {
                webClient.get().uri(uri).exchangeToMono { clientResponse ->
                val code = clientResponse.statusCode().value()
                recordResponse(code, clientResponse.headers().asHttpHeaders())
                clientResponse.bodyToMono(CongressBillsResponse::class.java)
                }
                .timeout(Duration.ofSeconds(10))
                .let { mono ->
                    val attempts = adaptiveRetryAttempts()
                    if (attempts > 0) mono.retryWhen(buildRetrySpec(attempts)) else mono
                }
                .awaitSingle()
            }
        }.getOrElse { e ->
            outcome = "error"
            logger.error("Error fetching bills from Congress API", e); CongressBillsResponse()
        }
    // Latency metric removed
        val summaries = response.bills.map { b -> CongressBillSummary(b.congress, b.number, b.type, b.title, b.introducedDate) }
        return CongressBillsPage(summaries)
    }

    @Cacheable(value = ["congress-bill-details"], key = "'congress-bill-details_'+#congress+'_'+#billType+'_'+#billNumber")
    override suspend fun getBillDetails(congress: Int, billType: String, billNumber: String): CongressBillDetail? {
    val start = System.nanoTime()
    val uri = UriComponentsBuilder.fromUriString(props.baseUrl)
            .path("/bill/{congress}/{type}/{number}")
            .queryParam("api_key", props.key)
            .queryParam("format", "json")
            .buildAndExpand(congress, billType.lowercase(), billNumber)
            .toUri()
        logger.debug("Fetching bill details from Congress API: {}", sanitizeUri(uri.toString()))
        var outcome = "success"
        val billSummary = runCatching {
            val response = withBreaker("getBillDetails", { null }) {
                webClient.get().uri(uri).exchangeToMono { clientResponse ->
                val code = clientResponse.statusCode().value()
                recordResponse(code, clientResponse.headers().asHttpHeaders())
                clientResponse.bodyToMono(CongressBillDetailWrapper::class.java)
                }
                .timeout(Duration.ofSeconds(10))
                .let { mono ->
                    val attempts = adaptiveRetryAttempts()
                    if (attempts > 0) mono.retryWhen(buildRetrySpec(attempts)) else mono
                }
                .awaitSingleOrNull()
            }
            response?.bill?.let { b ->
                CongressBillSummary(
                    congress = b.congress,
                    number = b.number,
                    type = b.type,
                    title = b.title,
                    introducedDate = b.introducedDate,
                )
            }
        }.getOrElse { e ->
            outcome = "error"
            logger.error("Error fetching bill details from Congress API", e); null
        }
    // Latency metric removed
        return billSummary?.let { CongressBillDetail(it) }
    }

    override suspend fun ping(): Boolean {
    val start = System.nanoTime()
        // Use a minimal request with limit=1; rely on same endpoint but cheap response size
        val dateStr = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
    val uri = UriComponentsBuilder.fromUriString(props.baseUrl)
            .path("/bill")
            .queryParam("api_key", props.key)
            .queryParam("fromDateTime", "${dateStr}T00:00:00Z")
            .queryParam("limit", 1)
            .queryParam("offset", 0)
            .queryParam("format", "json")
            .build().toUri()
    var outcome = "success"
        val result = runCatching {
            withBreaker("ping", { false }) {
                webClient.get().uri(uri).exchangeToMono { clientResponse ->
                val code = clientResponse.statusCode().value()
                recordResponse(code, clientResponse.headers().asHttpHeaders())
                clientResponse.bodyToMono(CongressBillsResponse::class.java)
                }
                .timeout(Duration.ofSeconds(5))
                .retryWhen(buildRetrySpec(1))
                .awaitSingleOrNull() != null
            }
        }.getOrDefault(false)
    if (!result) outcome = "error"
    return result
    }

    private fun sanitizeUri(uri: String): String = uri.replace(Regex("(api_key=)[^&]+"), "$1***")

    /** Lightweight snapshot for health endpoint without triggering network calls. */
    fun healthSnapshot(): Map<String, Any?> = mapOf(
        "circuitState" to when (breakerStateHolder.get()) { 0 -> "CLOSED" else -> "OPEN" },
        "consecutiveFailures" to consecutiveFailureHolder.get(),
        "rateLimitRemaining" to remainingGaugeHolder.get().takeIf { it >= 0 },
        "rateLimitLimit" to limitGaugeHolder.get().takeIf { it >= 0 },
        "rateLimitResetSeconds" to resetGaugeHolder.get().takeIf { it >= 0 },
        "rateLimitRemainingPct" to run {
            val limit = limitGaugeHolder.get(); val rem = remainingGaugeHolder.get();
            if (limit > 0 && rem >= 0) (rem.toDouble() / limit.toDouble()) * 100.0 else null
        },
        "last429Epoch" to last429EpochHolder.get().takeIf { it >= 0 },
        "circuitOpenDurationSeconds" to run {
            if (breakerStateHolder.get() == 1 && breakerOpenedAt > 0) {
                (System.nanoTime() - breakerOpenedAt).toDouble() / 1_000_000_000.0
            } else 0.0
        }
    )
}
