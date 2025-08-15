package com.legistrack.controller

import com.legistrack.dto.DocumentDetailDto
import com.legistrack.dto.DocumentSummaryDto
import com.legistrack.dto.InvalidateAnalysisRequest
import com.legistrack.service.DataIngestionService
import com.legistrack.service.DocumentService
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val dataIngestionService: DataIngestionService,
) {
    @GetMapping
    fun getAllDocuments(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "introductionDate") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<Page<DocumentSummaryDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val sort = if (sortDir.lowercase() == "desc") Sort.by(sortBy).descending() else Sort.by(sortBy).ascending()

        val pageable = PageRequest.of(safePage, safeSize, sort)
        val documents = documentService.getAllDocuments(pageable)

        return ResponseEntity.ok(documents)
    }

    @GetMapping("/{id}")
    fun getDocumentById(
        @PathVariable id: Long,
    ): ResponseEntity<DocumentDetailDto> {
        val document = documentService.getDocumentById(id)
        return if (document != null) {
            ResponseEntity.ok(document)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search")
    fun searchDocuments(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<DocumentSummaryDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(safePage, safeSize, Sort.by("introductionDate").descending())
        val results = documentService.searchDocuments(query, pageable)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/tag/{tag}")
    fun getDocumentsByTag(
        @PathVariable tag: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<DocumentSummaryDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(safePage, safeSize, Sort.by("introductionDate").descending())
        val results = documentService.findByIndustryTag(tag, pageable)
        return ResponseEntity.ok(results)
    }

    @PostMapping("/analysis/invalidate")
    fun invalidateAnalysis(
        @RequestBody request: InvalidateAnalysisRequest,
    ): ResponseEntity<Map<String, Any>> {
        val success = documentService.invalidateAnalysis(request.analysisId)

        return if (success) {
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Analysis invalidated successfully",
                ),
            )
        } else {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "Failed to invalidate analysis",
                ),
            )
        }
    }

    @PostMapping("/ingest")
    suspend fun triggerDataIngestion(
        @RequestParam(required = false) fromDate: LocalDate?,
    ): ResponseEntity<Map<String, Any>> =
        try {
            val ingestedCount =
                dataIngestionService.ingestRecentDocuments(
                    fromDate ?: LocalDate.now().minusDays(7),
                )

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Data ingestion completed",
                    "documentsIngested" to ingestedCount,
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "Data ingestion failed: ${e.message}",
                ),
            )
        }

    @PostMapping("/{id}/refresh")
    suspend fun refreshDocument(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "false") reanalyze: Boolean,
    ): ResponseEntity<Map<String, Any>> =
        try {
            dataIngestionService.refreshDocument(id, reanalyze)
            ResponseEntity.ok(mapOf("success" to true))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to (e.message ?: "error")))
        }

    @PostMapping("/{id}/analyze")
    suspend fun analyzeDocument(@PathVariable id: Long): ResponseEntity<Any> =
        try {
            val analyzed = documentService.analyzeDocument(id)
            if (analyzed != null) ResponseEntity.ok(analyzed) else ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to (e.message ?: "error")))
        }
}
