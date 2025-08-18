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

package com.legistrack.ingestion

import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.CongressPort
import com.legistrack.domain.port.DocumentRepositoryPort
import com.legistrack.domain.port.IngestionRunRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Orchestrates retrieval of recent legislative documents from external sources
 * and persistence into the repository via domain ports. AI analysis triggering
 * will be handled in Phase 5 (ai-analysis module) and is intentionally omitted here.
 */
@Service
open class DataIngestionService(
    private val congressPort: CongressPort,
    private val documentRepositoryPort: DocumentRepositoryPort,
    private val ingestionRunRepositoryPort: IngestionRunRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(DataIngestionService::class.java)
    // Metrics removed per requirement to strip instrumentation.

    /**
     * Build a normalized synthetic bill ID using canonical pattern TYPE+NUMBER-CONGRESS.
     * Ensures uppercasing of type and zero trimming of number input if present.
     */
    private fun buildBillId(type: String?, number: String?, congressSession: Int?): String? {
        if (number.isNullOrBlank()) return null
        val normType = type?.trim()?.uppercase().orEmpty()
        val normNumber = number.trim()
        return buildString {
            if (normType.isNotEmpty()) append(normType)
            append(normNumber)
            if (congressSession != null) append("-").append(congressSession)
        }
    }

    /**
     * Ingest recent documents updated since the provided date (default 7 days ago).
     * Returns number of new documents persisted. Existing documents (matched by
     * billId + congress composite) are skipped for now (TODO phase: add update path).
     */
    @Transactional
    suspend fun ingestRecentDocuments(fromDate: LocalDate): Int {
        var runId: Long? = null
        return try {
        // Idempotency: skip if a successful run already exists for this fromDate
        ingestionRunRepositoryPort.findSuccessful(fromDate)?.let {
            logger.info("[Ingestion] Skipping ingestion for fromDate={} (already successful run id={})", fromDate, it.id)
            // Skipped ingestion (metrics removed)
            return 0
        }
        val run = ingestionRunRepositoryPort.create(fromDate)
        runId = run?.id
        logger.info("[Ingestion] Fetching recent bills since {} (runId={})", fromDate, runId)
        var offset = 0
        val pageSize = 50
        var totalPersisted = 0
        var pageFetch = 0
        val maxPages = 20 // safety cap (configurable future)
    // Timer instrumentation removed
        while (pageFetch < maxPages) {
            val page = congressPort.getRecentBills(fromDate, offset, pageSize)
            if (page.bills.isEmpty()) break
            page.bills.forEach { bill ->
                val syntheticBillId = buildBillId(bill.type, bill.number, bill.congress) ?: return@forEach
                if (documentRepositoryPort.findByBillId(syntheticBillId) == null) {
                    val doc = Document(
                        billId = syntheticBillId,
                        title = bill.title ?: syntheticBillId,
                        status = null,
                        introductionDate = null,
                        congressSession = bill.congress,
                        billType = bill.type,
                    )
                    documentRepositoryPort.save(doc)
                    totalPersisted += 1
                }
            }
            if (page.bills.size < pageSize) break // last page
            offset += pageSize
            pageFetch += 1
        }
        logger.info("[Ingestion] Completed ingestion: {} new documents (pages processed: {}, runId={})", totalPersisted, pageFetch + 1, runId)
        runId?.let { ingestionRunRepositoryPort.markSuccess(it, totalPersisted) }
        totalPersisted
    } catch (e: Exception) {
        logger.error("[Ingestion] Failure during recent documents ingestion: ${e.message}", e)
        runId?.let { ingestionRunRepositoryPort.markFailure(it, e.message) }
        0
    }
    }

    /**
     * Refresh a single document by re-fetching its details (placeholder: minimal implementation).
     * Currently returns a boolean success flag.
     */
    @Transactional
    suspend fun refreshDocument(id: Long, @Suppress("UNUSED_PARAMETER") reanalyze: Boolean): Boolean {
        // Phase 5 will hook AI analysis; for now just ensure document exists.
        val existing = documentRepositoryPort.findByIdWithDetails(id)
        return existing != null
    }
}
