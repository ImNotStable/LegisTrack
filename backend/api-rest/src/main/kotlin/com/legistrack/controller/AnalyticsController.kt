package com.legistrack.controller

import com.legistrack.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(private val documentService: DocumentService) {
    @GetMapping("/summary")
    fun getSummary(): ResponseEntity<Map<String, Any>> = ResponseEntity.ok(documentService.getAnalyticsSummary())
}
