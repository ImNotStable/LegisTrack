package com.legistrack.aianalysis

import com.legistrack.domain.port.AiAnalysisRepositoryPort
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.DocumentRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import io.micrometer.core.instrument.MeterRegistry
import kotlin.system.measureTimeMillis

/**
 * Phase 5: Central AI analysis orchestration service.
 * Delegates to AiModelPort (Ollama adapter) for generation while owning prompt construction.
 */
@Service
class AiAnalysisService(
    private val aiModelPort: AiModelPort,
    private val aiAnalysisRepositoryPort: AiAnalysisRepositoryPort,
    private val documentRepositoryPort: DocumentRepositoryPort,
    private val meterRegistry: MeterRegistry? = null,
) {
    private val log = LoggerFactory.getLogger(AiAnalysisService::class.java)
    private val prefix = "ai.analysis"
    private val requestCounter by lazy { meterRegistry?.counter("$prefix.requests") }
    private val failureCounter by lazy { meterRegistry?.counter("$prefix.failures") }
    private val promptCharsCounter by lazy { meterRegistry?.counter("$prefix.prompt.chars") }
    private val responseCharsCounter by lazy { meterRegistry?.counter("$prefix.response.chars") }
    private val latencyTimer by lazy { meterRegistry?.timer("$prefix.latency") }
    // Gauges for monitoring rolling success rate and size of last response chars (excludes prompts)
    @Volatile private var recentSuccessCount: Long = 0
    @Volatile private var recentFailureCount: Long = 0
    @Volatile private var lastResponseChars: Long = 0
    init {
        meterRegistry?.gauge("$prefix.success.rate", this) { inst ->
            val total = inst.recentSuccessCount + inst.recentFailureCount
            if (total == 0L) 0.0 else inst.recentSuccessCount.toDouble() / total.toDouble()
        }
        meterRegistry?.gauge("$prefix.last.response.chars", this) { inst -> inst.lastResponseChars.toDouble() }
    }

    suspend fun generateGeneralEffect(title: String, summary: String?): String? {
        if (!aiModelPort.isServiceReady()) return null
        val prompt = buildGeneralEffectPrompt(title, summary)
        return aiModelPort.generateAnalysis(prompt)
    }

    suspend fun generateEconomicEffect(title: String, summary: String?): String? {
        if (!aiModelPort.isServiceReady()) return null
        val prompt = buildEconomicEffectPrompt(title, summary)
        return aiModelPort.generateAnalysis(prompt)
    }

    suspend fun generateIndustryTags(title: String, summary: String?): List<String> {
        if (!aiModelPort.isServiceReady()) return emptyList()
        val prompt = buildIndustryTagsPrompt(title, summary)
        return aiModelPort.generateAnalysis(prompt)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(MAX_TAGS)
            ?: emptyList()
    }

    private fun buildGeneralEffectPrompt(title: String, summary: String?): String =
        "Provide a concise general effect analysis (max 120 words) of the bill titled '$title'. Summary: ${summary ?: "(no summary)"}"

    private fun buildEconomicEffectPrompt(title: String, summary: String?): String =
        "Analyze potential economic impacts (max 120 words) for the bill titled '$title'. Summary: ${summary ?: "(no summary)"}"

    private fun buildIndustryTagsPrompt(title: String, summary: String?): String =
        "List up to $MAX_TAGS comma-separated industry sectors impacted by '$title'. Summary: ${summary ?: "(no summary)"}. Only list the sectors, no explanations."

    companion object { private const val MAX_TAGS = 5; private const val DEFAULT_MODEL_NAME = "gpt-oss:20b" }

    /**
     * Generate all analysis components for a document and persist a new AiAnalysis row
     * only if at least one field has meaningful content. Returns the saved entity or null.
     * Respects service readiness; returns null early if model not ready.
     */
    suspend fun generateAndPersist(document: Document, modelName: String = DEFAULT_MODEL_NAME): AiAnalysis? {
        if (!aiModelPort.isServiceReady()) {
            log.debug("AI model service not ready, skipping analysis for document {}", document.billId)
            return null
        }
        val title = document.title
        val summary = document.officialSummary

        val prompts = mutableListOf<String>()
    var general: String?
    var economic: String?
    var tags: List<String>
        val elapsed = measureTimeMillis {
            general = try {
                val p = buildGeneralEffectPrompt(title, summary).also { prompts += it }
                p.let { promptCharsCounter?.increment(it.length.toDouble()) }
                generateGeneralEffect(title, summary)
            } catch (e: Exception) {
                failureCounter?.increment(); log.warn("Failed general effect generation for {}: {}", document.billId, e.message); null
            }
            economic = try {
                val p = buildEconomicEffectPrompt(title, summary).also { prompts += it }
                p.let { promptCharsCounter?.increment(it.length.toDouble()) }
                generateEconomicEffect(title, summary)
            } catch (e: Exception) {
                failureCounter?.increment(); log.warn("Failed economic effect generation for {}: {}", document.billId, e.message); null
            }
            tags = try {
                val p = buildIndustryTagsPrompt(title, summary).also { prompts += it }
                p.let { promptCharsCounter?.increment(it.length.toDouble()) }
                generateIndustryTags(title, summary)
            } catch (e: Exception) {
                failureCounter?.increment(); log.warn("Failed industry tags generation for {}: {}", document.billId, e.message); emptyList()
            }
        }
        latencyTimer?.record(java.time.Duration.ofMillis(elapsed))
        var responseCharsTotal = 0
        sequenceOf(general, economic).filterNotNull().forEach { resp ->
            responseCharsCounter?.increment(resp.length.toDouble())
            responseCharsTotal += resp.length
        }
        val tagChars = tags.sumOf { it.length }
        responseCharsCounter?.increment(tagChars.toDouble())
        responseCharsTotal += tagChars
        requestCounter?.increment()

        val hasContent = !general.isNullOrBlank() || !economic.isNullOrBlank() || tags.isNotEmpty()
        if (!hasContent) {
            log.debug("No meaningful content generated for document {}, skipping persistence", document.billId)
            recentFailureCount++
            return null
        }
        
        val entity = AiAnalysis(
            documentId = requireNotNull(document.id) { "Document ID required to persist analysis" },
            generalEffectText = general,
            economicEffectText = economic,
            industryTags = tags,
            isValid = true,
            modelUsed = modelName,
        )
        
        return try {
            val savedEntity = aiAnalysisRepositoryPort.save(entity)
            log.info("Successfully persisted AI analysis for document {} with model {}", document.billId, modelName)
            recentSuccessCount++
            lastResponseChars = responseCharsTotal.toLong()
            savedEntity
        } catch (e: Exception) {
            failureCounter?.increment()
            log.error("Failed to persist AI analysis for document {}: {}", document.billId, e.message, e)
            recentFailureCount++
            null
        }
    }
}
