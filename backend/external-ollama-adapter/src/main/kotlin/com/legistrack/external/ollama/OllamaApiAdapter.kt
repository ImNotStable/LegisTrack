package com.legistrack.external.ollama

import com.legistrack.domain.port.AiModelPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration

// Internal minimal request/response representations (decoupled from original dto.external package)
private data class OllamaOptions(val temperature: Double, val topP: Double, val numCtx: Int)
private data class OllamaGenerateRequest(val model: String, val prompt: String, val stream: Boolean, val options: OllamaOptions)
private data class OllamaGenerateResponse(val response: String?)
private data class OllamaModelTag(val name: String)
private data class OllamaTagsResponse(val models: List<OllamaModelTag> = emptyList())
private data class OllamaPullRequest(val name: String)

@Component
open class OllamaApiAdapter(
    private val webClient: WebClient,
    private val props: OllamaProperties,
    private val meterRegistry: MeterRegistry? = null,
) : AiModelPort {
    companion object {
        private val logger = LoggerFactory.getLogger(OllamaApiAdapter::class.java)
        private const val DEFAULT_TOP_P = 0.9
        private const val DEFAULT_CONTEXT_SIZE = 4096
        private const val RETRY_ATTEMPTS = 3L
        private const val RETRY_DELAY_SECONDS = 2L
        private const val MAX_INDUSTRY_TAGS = 5
        private const val SERVICE_AVAILABILITY_CHECK_INTERVAL_SECONDS = 30L
        private const val MODEL_DOWNLOAD_CHECK_INTERVAL_SECONDS = 30L
        private const val MAX_SERVICE_WAIT_MINUTES = 10L
        private const val MAX_MODEL_WAIT_MINUTES = 30L
    private const val METRIC_PREFIX = "ollama.api"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Metrics (lazy to avoid registry cost when not configured)
    private val requestsCounter by lazy { meterRegistry?.counter("$METRIC_PREFIX.requests") }
    private val errorsCounter by lazy { meterRegistry?.counter("$METRIC_PREFIX.errors") }
    private val latencyTimer: Timer? by lazy { meterRegistry?.timer("$METRIC_PREFIX.latency") }
    private val throttledCounter by lazy { meterRegistry?.counter("$METRIC_PREFIX.status.429") }

    @Volatile
    private var serviceReady = false

    @EventListener(ApplicationReadyEvent::class)
    fun initializeOllamaService() {
    val baseUrl = props.baseUrl
    if (baseUrl.isBlank()) {
            logger.info("Ollama base URL is not configured; AI analysis will be disabled")
            serviceReady = false
            return
        }
    if (!props.bootstrapEnabled) {
            logger.info("Ollama bootstrap disabled via configuration; skipping initialization")
            return
        }
    logger.info("Starting Ollama service initialization with model: {}", props.model)
        coroutineScope.launch {
            try {
                waitForOllamaService()
                ensureModelAvailable()
                serviceReady = true
                logger.info("Ollama service successfully initialized and ready")
            } catch (e: Exception) {
                logger.error("Failed to initialize Ollama service: {}", e.message, e)
                serviceReady = false
            }
        }
    }

    private suspend fun waitForOllamaService() {
        val maxAttempts = (MAX_SERVICE_WAIT_MINUTES * 60) / SERVICE_AVAILABILITY_CHECK_INTERVAL_SECONDS
        var attempts = 0
    val baseUrl = props.baseUrl
    logger.info("Waiting for Ollama service to become available at: {}", baseUrl)
        while (attempts < maxAttempts) {
            try {
                val response = webClient.get().uri("$baseUrl/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagsResponse::class.java)
                    .timeout(Duration.ofSeconds(10))
                    .awaitSingleOrNull()
                if (response != null) {
                    logger.info("Ollama service is now available")
                    return
                }
            } catch (e: Exception) {
                logger.debug("Ollama service not yet available (attempt {}/{}): {}", attempts + 1, maxAttempts, e.message)
            }
            attempts++
            delay(SERVICE_AVAILABILITY_CHECK_INTERVAL_SECONDS * 1000)
        }
        throw RuntimeException("Ollama service did not become available within ${MAX_SERVICE_WAIT_MINUTES} minutes")
    }

    private suspend fun ensureModelAvailable() {
        if (isModelAvailable()) {
            logger.info("Model {} is already available", props.model)
            return
        }
        logger.info("Model {} not found, initiating download", props.model)
        downloadModel()
        waitForModelDownload()
    }

    private suspend fun downloadModel() {
        try {
            val baseUrl = props.baseUrl
            val request = OllamaPullRequest(name = props.model)
            logger.info("Sending model download request for: {}", props.model)
            webClient.post().uri("${baseUrl}/api/pull")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofMinutes(5))
                .awaitSingleOrNull()
            logger.info("Model download request sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to initiate model download: {}", e.message, e)
            throw RuntimeException("Failed to download model: ${e.message}", e)
        }
    }

    private suspend fun waitForModelDownload() {
        val maxAttempts = (MAX_MODEL_WAIT_MINUTES * 60) / MODEL_DOWNLOAD_CHECK_INTERVAL_SECONDS
        var attempts = 0
    logger.info("Waiting for model {} download to complete", props.model)
        while (attempts < maxAttempts) {
            if (isModelAvailable()) {
                logger.info("Model {} download completed successfully", props.model)
                return
            }
            logger.debug("Model {} not yet available, waiting... (attempt {}/{})", props.model, attempts + 1, maxAttempts)
            attempts++
            delay(MODEL_DOWNLOAD_CHECK_INTERVAL_SECONDS * 1000)
        }
        throw RuntimeException("Model download did not complete within ${MAX_MODEL_WAIT_MINUTES} minutes")
    }

    override fun isServiceReady(): Boolean = serviceReady

    override suspend fun isModelAvailable(): Boolean = isModelAvailableInternal()

    override suspend fun generateAnalysis(prompt: String, temperature: Double): String? {
        if (!serviceReady) {
            logger.warn("Ollama service is not ready, skipping analysis generation")
            return null
        }
        val request = OllamaGenerateRequest(
            model = props.model,
            prompt = prompt,
            stream = false,
            options = OllamaOptions(
                temperature = temperature,
                topP = DEFAULT_TOP_P,
                numCtx = DEFAULT_CONTEXT_SIZE,
            ),
        )
    val baseUrl = props.baseUrl
    logger.debug("Sending request to Ollama: model={}, prompt length={}", props.model, prompt.length)
        requestsCounter?.increment()
        val start = System.nanoTime()
        val result = runCatching {
            webClient.post().uri("${baseUrl}/api/generate")
                .bodyValue(request)
                .retrieve()
                .onStatus({ s -> s.value() == 429 }) { response ->
                    throttledCounter?.increment(); response.createException()
                }
                .bodyToMono(OllamaGenerateResponse::class.java)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)).jitter(0.2))
                .awaitSingleOrNull()
                ?.response?.trim()
        }.onFailure { e: Throwable ->
            errorsCounter?.increment()
            logger.error("Error generating analysis with Ollama", e)
        }.getOrNull()
        latencyTimer?.record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS)
        return result
    }

    private suspend fun isModelAvailableInternal(): Boolean = try {
    val baseUrl = props.baseUrl
    val response = webClient.get().uri("${baseUrl}/api/tags")
            .retrieve()
            .onStatus({ s -> s.value() == 429 }) { response -> throttledCounter?.increment(); response.createException() }
            .bodyToMono(OllamaTagsResponse::class.java)
            .timeout(Duration.ofSeconds(10))
            .awaitSingleOrNull()
    response?.models?.any { model -> model.name == props.model } ?: false
    } catch (e: WebClientResponseException) {
        logger.debug("HTTP error checking Ollama model availability: {} {}", e.statusCode, e.message)
        false
    } catch (e: Exception) {
        logger.debug("Error checking Ollama model availability: {}", e.message)
        false
    }

    override suspend fun generateGeneralEffectAnalysis(billTitle: String, billSummary: String?): String? =
        generateAnalysis("Provide a general effect analysis of the bill titled '$billTitle'. Summary: ${billSummary ?: "(no summary)"}")

    override suspend fun generateEconomicEffectAnalysis(billTitle: String, billSummary: String?): String? =
        generateAnalysis("Analyze the potential economic effects of the bill titled '$billTitle'. Summary: ${billSummary ?: "(no summary)"}")

    override suspend fun generateIndustryTags(billTitle: String, billSummary: String?): List<String> =
        generateAnalysis(
            "List up to $MAX_INDUSTRY_TAGS comma-separated industry sectors impacted by the bill titled '$billTitle'. Summary: ${billSummary ?: "(no summary)"}. Only list the sectors, no explanations."
        )
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(MAX_INDUSTRY_TAGS)
            ?: emptyList()
}
