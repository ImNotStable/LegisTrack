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
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration

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

	@EventListener(ApplicationReadyEvent::class)
	fun initializeOllamaService() {
		if (baseUrl.isBlank()) {
			logger.info("Ollama base URL is not configured; AI analysis will be disabled")
			isServiceReady = false
			return
		}
		logger.info("Starting Ollama service initialization with model: {}", modelName)
		coroutineScope.launch {
			try {
				waitForOllamaService()
				ensureModelAvailable()
				isServiceReady = true
				logger.info("Ollama service successfully initialized and ready")
			} catch (e: Exception) {
				logger.error("Failed to initialize Ollama service: {}", e.message, e)
				isServiceReady = false
			}
		}
	}

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
						.timeout(Duration.ofSeconds(10))
						.awaitSingleOrNull()
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

	private suspend fun ensureModelAvailable() {
		if (isModelAvailable()) {
			logger.info("Model {} is already available", modelName)
			return
		}
		logger.info("Model {} not found, initiating download", modelName)
		downloadModel()
		waitForModelDownload()
	}

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
					.retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)).jitter(0.2))
					.awaitSingleOrNull()
			response?.response?.trim()
		} catch (e: Exception) {
			logger.error("Error generating analysis with Ollama", e)
			null
		}
	}

	suspend fun isModelAvailable(): Boolean =
		try {
			val response =
				webClient
					.get()
					.uri("$baseUrl/api/tags")
					.retrieve()
					.bodyToMono(OllamaTagsResponse::class.java)
					.timeout(Duration.ofSeconds(10))
					.awaitSingleOrNull()
			response?.models?.any { model -> model.name == modelName || model.name.substringBefore(":") == modelName.substringBefore(":") }
				?: false
		} catch (e: WebClientResponseException) {
			logger.debug("HTTP error checking Ollama model availability: {} {}", e.statusCode, e.message)
			false
		} catch (e: Exception) {
			logger.debug("Error checking Ollama model availability: {}", e.message)
			false
		}

	fun isServiceReady(): Boolean = isServiceReady

	suspend fun generateGeneralEffectAnalysis(
		billTitle: String,
		billSummary: String?,
	): String? {
		val prompt = buildGeneralEffectPrompt(billTitle, billSummary)
		return generateAnalysis(prompt)
	}

	suspend fun generateEconomicEffectAnalysis(
		billTitle: String,
		billSummary: String?,
	): String? {
		val prompt = buildEconomicEffectPrompt(billTitle, billSummary)
		return generateAnalysis(prompt)
	}

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
