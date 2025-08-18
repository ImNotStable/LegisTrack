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

package com.legistrack.aianalysis

import com.legistrack.domain.port.AiAnalysisRepositoryPort
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.DocumentRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
) {
    private val log = LoggerFactory.getLogger(AiAnalysisService::class.java)
    // Metrics removed per instrumentation strip requirement.

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

        var general: String?
        var economic: String?
        var tags: List<String>
        general = try {
            generateGeneralEffect(title, summary)
        } catch (e: Exception) {
            log.warn("Failed general effect generation for {}: {}", document.billId, e.message); null
        }
        economic = try {
            generateEconomicEffect(title, summary)
        } catch (e: Exception) {
            log.warn("Failed economic effect generation for {}: {}", document.billId, e.message); null
        }
        tags = try {
            generateIndustryTags(title, summary)
        } catch (e: Exception) {
            log.warn("Failed industry tags generation for {}: {}", document.billId, e.message); emptyList()
        }
    // Latency and size metrics removed

        val hasContent = !general.isNullOrBlank() || !economic.isNullOrBlank() || tags.isNotEmpty()
        if (!hasContent) {
            log.debug("No meaningful content generated for document {}, skipping persistence", document.billId)
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
            savedEntity
        } catch (e: Exception) {
            log.error("Failed to persist AI analysis for document {}: {}", document.billId, e.message, e)
            null
        }
    }
}
