package com.legistrack.controller

import com.legistrack.dto.external.CongressBill
import com.legistrack.dto.external.CongressSponsor
import com.legistrack.dto.external.CongressSummary
import com.legistrack.service.DataIngestionService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Test controller for verifying data ingestion functionality.
 * Provides endpoints to test the ingestion pipeline with mock data.
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = ["http://localhost:3000"])
class TestController(
    private val dataIngestionService: DataIngestionService
) {
    
    /**
     * Test endpoint to verify DTO deserialization and document processing.
     * Creates a mock bill and processes it through the ingestion pipeline.
     */
    @PostMapping("/mock-ingestion")
    fun testMockIngestion(): ResponseEntity<Map<String, Any>> {
        return try {
            // Create a mock Congress bill
            val mockBill = CongressBill(
                congress = 118,
                number = "1234",
                type = "HR",
                title = "Test Infrastructure Investment Act",
                introducedDate = "2024-12-01",
                sponsors = listOf(
                    CongressSponsor(
                        bioguideId = "T000001",
                        firstName = "Test",
                        lastName = "Representative",
                        party = "D",
                        state = "CA",
                        district = "1"
                    )
                ),
                summaries = listOf(
                    CongressSummary(
                        text = "This test bill would authorize funding for infrastructure improvements including roads, bridges, and broadband expansion in rural areas.",
                        versionCode = "00"
                    )
                )
            )
            
            // Process the mock bill
            val result = runBlocking {
                dataIngestionService.processBill(mockBill)
            }
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Mock bill processed successfully",
                "billProcessed" to result,
                "billId" to "${mockBill.type}${mockBill.number}-${mockBill.congress}"
            ))
            
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "Mock ingestion failed: ${e.message}",
                "error" to e.javaClass.simpleName
            ))
        }
    }
    
    /**
     * Test endpoint to verify system health and readiness.
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "message" to "Test endpoints are ready",
            "timestamp" to System.currentTimeMillis()
        ))
    }
}
