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
import org.springframework.web.bind.annotation.CrossOrigin
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
@CrossOrigin(origins = ["http://localhost:3000"])
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
        val sort =
            if (sortDir.lowercase() == "desc") {
                Sort.by(sortBy).descending()
            } else {
                Sort.by(sortBy).ascending()
            }

        val pageable = PageRequest.of(page, size, sort)
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
    fun triggerDataIngestion(
        @RequestParam(required = false) fromDate: LocalDate?,
    ): ResponseEntity<Map<String, Any>> =
        try {
            val ingestedCount =
                runBlocking {
                    dataIngestionService.ingestRecentDocuments(
                        fromDate ?: LocalDate.now().minusDays(7),
                    )
                }

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
}
