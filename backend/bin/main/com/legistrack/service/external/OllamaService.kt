package com.legistrack.service.external

import com.legistrack.dto.external.OllamaGenerateRequest
import com.legistrack.dto.external.OllamaGenerateResponse
import com.legistrack.dto.external.OllamaOptions
import com.legistrack.dto.external.OllamaPullRequest
import com.legistrack.dto.external.OllamaTagsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Service for interacting with Ollama AI model API.
 *
 * Provides methods for generating AI analyses of legislative documents
 * using the configured Ollama model.
 */
@Service
class OllamaService(
    private val webClient: WebClient,
    @Value("\${app.ollama.base-url}") private val baseUrl: String,
    @Value("\${app.ollama.model}") private val modelName: String,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OllamaService::class.java)
        private const val DEFAULT_TEMPERATURE = 0.7
        private const val DEFAULT_TOP_P = 0.9
        private const val DEFAULT_CONTEXT_SIZE = 4096
        private const val RETRY_ATTEMPTS = 3L
        private const val RETRY_DELAY_SECONDS = 2L
        private const val MAX_INDUSTRY_TAGS = 5
        private const val SERVICE_AVAILABILITY_CHECK_INTERVAL_SECONDS = 30L
        private const val MODEL_DOWNLOAD_CHECK_INTERVAL_SECONDS = 30L
        private const val MAX_SERVICE_WAIT_MINUTES = 10L
        private const val MAX_MODEL_WAIT_MINUTES = 30L
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var isServiceReady = false

    /**
     * Initialize Ollama service on application startup.
     * This method will wait for Ollama to be available and ensure the required model is downloaded.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun initializeOllamaService() {
        logger.info("Starting Ollama service initialization with model: {}", modelName)

        // Launch coroutine for async initialization
        coroutineScope.launch {
            try {
                // Wait for Ollama service to be available
                waitForOllamaService()

                // Ensure model is available (download if necessary)
                ensureModelAvailable()

                isServiceReady = true
                logger.info("Ollama service successfully initialized and ready")
            } catch (e: Exception) {
                logger.error("Failed to initialize Ollama service: {}", e.message, e)
                isServiceReady = false
            }
        }
    }

    /**
     * Waits for Ollama service to become available.
     */
    private suspend fun waitForOllamaService() {
        val maxAttempts = (MAX_SERVICE_WAIT_MINUTES * 60) / SERVICE_AVAILABILITY_CHECK_INTERVAL_SECONDS
        var attempts = 0

        logger.info("Waiting for Ollama service to become available at: {}", baseUrl)

        while (attempts < maxAttempts) {
            try {
                val response =
                    webClient
                        .get()
                        .uri("$baseUrl/api/tags")
                        .retrieve()
                        .bodyToMono(OllamaTagsResponse::class.java)
                        .block(Duration.ofSeconds(10))

                if (response != null) {
                    logger.info("Ollama service is now available")
                    return
                }
            } catch (e: Exception) {
                logger.debug(
                    "Ollama service not yet available (attempt {}/{}): {}",
                    attempts + 1,
                    maxAttempts,
                    e.message,
                )
            }

            attempts++
            delay(SERVICE_AVAILABILITY_CHECK_INTERVAL_SECONDS * 1000)
        }

        throw RuntimeException("Ollama service did not become available within ${MAX_SERVICE_WAIT_MINUTES} minutes")
    }

    /**
     * Ensures the required model is available, downloading it if necessary.
     */
    private suspend fun ensureModelAvailable() {
        if (isModelAvailable()) {
            logger.info("Model {} is already available", modelName)
            return
        }

        logger.info("Model {} not found, initiating download", modelName)
        downloadModel()
        waitForModelDownload()
    }

    /**
     * Downloads the required model from Ollama.
     */
    private suspend fun downloadModel() {
        try {
            val request = OllamaPullRequest(name = modelName)

            logger.info("Sending model download request for: {}", modelName)

            webClient
                .post()
                .uri("$baseUrl/api/pull")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofMinutes(5))

            logger.info("Model download request sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to initiate model download: {}", e.message, e)
            throw RuntimeException("Failed to download model: ${e.message}", e)
        }
    }

    /**
     * Waits for model download to complete.
     */
    private suspend fun waitForModelDownload() {
        val maxAttempts = (MAX_MODEL_WAIT_MINUTES * 60) / MODEL_DOWNLOAD_CHECK_INTERVAL_SECONDS
        var attempts = 0

        logger.info("Waiting for model {} download to complete", modelName)

        while (attempts < maxAttempts) {
            if (isModelAvailable()) {
                logger.info("Model {} download completed successfully", modelName)
                return
            }

            logger.debug(
                "Model {} not yet available, waiting... (attempt {}/{})",
                modelName,
                attempts + 1,
                maxAttempts,
            )

            attempts++
            delay(MODEL_DOWNLOAD_CHECK_INTERVAL_SECONDS * 1000)
        }

        throw RuntimeException("Model download did not complete within ${MAX_MODEL_WAIT_MINUTES} minutes")
    }

    /**
     * Generates text analysis using the configured Ollama model.
     *
     * @param prompt The prompt to send to the model
     * @return Generated text response or null if generation failed
     */
    suspend fun generateAnalysis(prompt: String): String? {
        if (!isServiceReady) {
            logger.warn("Ollama service is not ready, skipping analysis generation")
            return null
        }

        val request =
            OllamaGenerateRequest(
                model = modelName,
                prompt = prompt,
                stream = false,
                options =
                    OllamaOptions(
                        temperature = DEFAULT_TEMPERATURE,
                        topP = DEFAULT_TOP_P,
                        numCtx = DEFAULT_CONTEXT_SIZE,
                    ),
            )

        logger.debug("Sending request to Ollama: model={}, prompt length={}", modelName, prompt.length)

        return try {
            val response =
                webClient
                    .post()
                    .uri("$baseUrl/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaGenerateResponse::class.java)
                    .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                    .block()

            response?.response?.trim()
        } catch (e: Exception) {
            logger.error("Error generating analysis with Ollama", e)
            null
        }
    }

    /**
     * Checks if the configured model is available in Ollama.
     *
     * @return true if model is available, false otherwise
     */
    suspend fun isModelAvailable(): Boolean =
        try {
            val response =
                webClient
                    .get()
                    .uri("$baseUrl/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagsResponse::class.java)
                    .block(Duration.ofSeconds(10))

            response?.models?.any { model -> model.name.contains(modelName) } ?: false
        } catch (e: WebClientResponseException) {
            logger.debug("HTTP error checking Ollama model availability: {} {}", e.statusCode, e.message)
            false
        } catch (e: Exception) {
            logger.debug("Error checking Ollama model availability: {}", e.message)
            false
        }

    /**
     * Checks if the Ollama service is ready for use.
     *
     * @return true if service is ready, false otherwise
     */
    fun isServiceReady(): Boolean = isServiceReady

    /**
     * Generates a general effect analysis for a legislative bill.
     *
     * @param billTitle Title of the bill
     * @param billSummary Optional official summary
     * @return AI-generated general effect analysis
     */
    suspend fun generateGeneralEffectAnalysis(
        billTitle: String,
        billSummary: String?,
    ): String? {
        val prompt = buildGeneralEffectPrompt(billTitle, billSummary)
        return generateAnalysis(prompt)
    }

    /**
     * Generates an economic effect analysis for a legislative bill.
     *
     * @param billTitle Title of the bill
     * @param billSummary Optional official summary
     * @return AI-generated economic effect analysis
     */
    suspend fun generateEconomicEffectAnalysis(
        billTitle: String,
        billSummary: String?,
    ): String? {
        val prompt = buildEconomicEffectPrompt(billTitle, billSummary)
        return generateAnalysis(prompt)
    }

    /**
     * Generates industry tags for a legislative bill.
     *
     * @param billTitle Title of the bill
     * @param billSummary Optional official summary
     * @return List of relevant industry tags
     */
    suspend fun generateIndustryTags(
        billTitle: String,
        billSummary: String?,
    ): List<String> {
        val prompt = buildIndustryTagsPrompt(billTitle, billSummary)
        val response = generateAnalysis(prompt)

        return response
            ?.split(",")
            ?.map { tag -> tag.trim() }
            ?.filter { tag -> tag.isNotBlank() }
            ?.take(MAX_INDUSTRY_TAGS)
            .orEmpty()
    }

    /**
     * Builds the prompt for general effect analysis.
     */
    private fun buildGeneralEffectPrompt(
        billTitle: String,
        billSummary: String?,
    ): String =
        buildString {
            appendLine(
                "Analyze the following U.S. legislative bill and provide a concise summary of its general effect on society and governance.",
            )
            appendLine()
            appendLine("Bill Title: $billTitle")
            if (!billSummary.isNullOrBlank()) {
                appendLine("Official Summary: $billSummary")
            }
            appendLine()
            appendLine("Please provide a clear, objective analysis of the bill's general impact in 2-3 paragraphs. Focus on:")
            appendLine("1. What the bill aims to accomplish")
            appendLine("2. Who would be affected by this legislation")
            appendLine("3. How it might change existing laws or policies")
            appendLine()
            appendLine("Keep the analysis factual and avoid political bias.")
        }

    /**
     * Builds the prompt for economic effect analysis.
     */
    private fun buildEconomicEffectPrompt(
        billTitle: String,
        billSummary: String?,
    ): String =
        buildString {
            appendLine("Analyze the following U.S. legislative bill and provide a focused assessment of its economic impact.")
            appendLine()
            appendLine("Bill Title: $billTitle")
            if (!billSummary.isNullOrBlank()) {
                appendLine("Official Summary: $billSummary")
            }
            appendLine()
            appendLine("Please provide an economic analysis in 2-3 paragraphs covering:")
            appendLine("1. Potential fiscal impact (costs, savings, revenue effects)")
            appendLine("2. Effects on businesses, consumers, or specific market sectors")
            appendLine("3. Employment and economic growth implications")
            appendLine("4. Long-term economic consequences")
            appendLine()
            appendLine("Focus on quantifiable impacts where possible and avoid political commentary.")
        }

    /**
     * Builds the prompt for industry tags generation.
     */
    private fun buildIndustryTagsPrompt(
        billTitle: String,
        billSummary: String?,
    ): String =
        buildString {
            appendLine("Analyze the following U.S. legislative bill and identify the primary industries or sectors that would be affected.")
            appendLine()
            appendLine("Bill Title: $billTitle")
            if (!billSummary.isNullOrBlank()) {
                appendLine("Official Summary: $billSummary")
            }
            appendLine()
            appendLine("Provide a comma-separated list of industry tags. Use broad industry categories such as:")
            appendLine(
                "Healthcare, Technology, Finance, Energy, Agriculture, Transportation, Education, Defense, Environment, Manufacturing, etc.",
            )
            appendLine()
            appendLine("Limit to the 3-5 most relevant industries. Only provide the industry names, separated by commas.")
        }
}
