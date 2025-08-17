package com.legistrack.controller

import com.legistrack.domain.common.Page
import com.legistrack.api.ErrorResponse
import com.legistrack.api.NotFoundException
import com.legistrack.domain.dto.DocumentSummaryDto
import com.legistrack.domain.dto.InvalidateAnalysisRequest
import com.legistrack.ingestion.DataIngestionService
import com.legistrack.service.DocumentService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.micrometer.core.instrument.MeterRegistry

@RestController
@RequestMapping("/api/documents")
class DocumentsController(
    private val documentService: DocumentService,
    private val dataIngestionService: DataIngestionService,
    private val meterRegistry: MeterRegistry? = null,
) {
    private val allowedSortFields = setOf("introductionDate", "createdAt", "title")

    private fun normalizeSort(sortBy: String): String = if (allowedSortFields.contains(sortBy)) sortBy else "introductionDate"

    @GetMapping
    fun getAllDocuments(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "introductionDate") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<Any> {
    if (page < 0) return ResponseEntity.badRequest().body(ErrorResponse(message = "Page index must be >= 0"))
    if (size !in 1..100) return ResponseEntity.badRequest().body(ErrorResponse(message = "Size must be between 1 and 100"))
        val safePage = page
        val safeSize = size
        val normalized = normalizeSort(sortBy).also {
            if (!allowedSortFields.contains(sortBy)) {
                meterRegistry?.counter("congress.api.documents.sort.rejected", "requested", sortBy)?.increment()
            }
        }
        val sort = if (sortDir.lowercase() == "desc") Sort.by(normalized).descending() else Sort.by(normalized).ascending()
        val pageable = PageRequest.of(safePage, safeSize, sort)
    val documents = documentService.getAllDocuments(pageable)
    return ResponseEntity.ok().body(documents as Any)
    }

    @GetMapping("/{id}")
    fun getDocumentById(@PathVariable id: Long): ResponseEntity<Any> =
        documentService.getDocumentById(id)?.let { ResponseEntity.ok(it) } ?: throw NotFoundException("Document not found")

    @GetMapping("/search")
    fun searchDocuments(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Any> {
    if (page < 0) return ResponseEntity.badRequest().body(ErrorResponse(message = "Page index must be >= 0"))
    if (size !in 1..100) return ResponseEntity.badRequest().body(ErrorResponse(message = "Size must be between 1 and 100"))
    val safePage = page
    val safeSize = size
        val pageable = PageRequest.of(safePage, safeSize, Sort.by("introductionDate").descending())
    return ResponseEntity.ok().body(documentService.searchDocuments(query, pageable) as Any)
    }

    @GetMapping("/tag/{tag}")
    fun getDocumentsByTag(
        @PathVariable tag: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Any> {
    if (page < 0) return ResponseEntity.badRequest().body(ErrorResponse(message = "Page index must be >= 0"))
    if (size !in 1..100) return ResponseEntity.badRequest().body(ErrorResponse(message = "Size must be between 1 and 100"))
    val safePage = page
    val safeSize = size
        val pageable = PageRequest.of(safePage, safeSize, Sort.by("introductionDate").descending())
    return ResponseEntity.ok().body(documentService.findByIndustryTag(tag, pageable) as Any)
    }

    @PostMapping("/analysis/invalidate")
    fun invalidateAnalysis(@RequestBody request: InvalidateAnalysisRequest): ResponseEntity<Any> {
        val success = documentService.invalidateAnalysis(request.analysisId)
        return if (success) ResponseEntity.ok(mapOf("success" to true, "message" to "Analysis invalidated successfully"))
        else ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Failed to invalidate analysis"))
    }

    @PostMapping("/ingest")
    suspend fun triggerDataIngestion(@RequestParam(required = false) fromDate: java.time.LocalDate?): ResponseEntity<Any> {
        val ingestedCount = dataIngestionService.ingestRecentDocuments(fromDate ?: java.time.LocalDate.now().minusDays(7))
        return ResponseEntity.ok(mapOf("success" to true, "message" to "Data ingestion completed", "documentsIngested" to ingestedCount))
    }

    @PostMapping("/{id}/refresh")
    suspend fun refreshDocument(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "false") reanalyze: Boolean,
    ): ResponseEntity<Any> {
        dataIngestionService.refreshDocument(id, reanalyze)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @PostMapping("/{id}/analyze")
    suspend fun analyzeDocument(@PathVariable id: Long): ResponseEntity<Any> {
        val analyzed = documentService.analyzeDocument(id)
        return if (analyzed != null) ResponseEntity.ok(analyzed) else throw NotFoundException("Document not found")
    }
}
