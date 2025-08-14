package com.legistrack.controller

import com.legistrack.dto.external.CongressBill
import com.legistrack.dto.external.CongressSponsor
import com.legistrack.dto.external.CongressSummary
import com.legistrack.service.DataIngestionService
import com.legistrack.service.external.OllamaService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Test controller for verifying data ingestion functionality.
 * Provides endpoints to test the ingestion pipeline with mock data.
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = ["http://localhost:3000"])
class TestController(
    private val dataIngestionService: DataIngestionService,
    private val ollamaService: OllamaService,
) {
    /**
     * Test endpoint to verify DTO deserialization and document processing.
     * Creates a mock bill and processes it through the ingestion pipeline.
     */
    @PostMapping("/mock-ingestion")
    fun testMockIngestion(): ResponseEntity<Map<String, Any>> =
        try {
            // Create a mock Congress bill
            val mockBill =
                CongressBill(
                    congress = 118,
                    number = "1234",
                    type = "HR",
                    title = "Test Infrastructure Investment Act",
                    introducedDate = "2024-12-01",
                    sponsors =
                        listOf(
                            CongressSponsor(
                                bioguideId = "T000001",
                                firstName = "Test",
                                lastName = "Representative",
                                party = "D",
                                state = "CA",
                                district = "1",
                            ),
                        ),
                    summaries =
                        listOf(
                            CongressSummary(
                                text =
                                    "This test bill would authorize funding for infrastructure improvements " +
                                        "including roads, bridges, and broadband expansion in rural areas.",
                                versionCode = "00",
                            ),
                        ),
                )

            // Process the mock bill
            val result =
                runBlocking {
                    dataIngestionService.processBill(mockBill)
                }

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Mock bill processed successfully",
                    "billProcessed" to result,
                    "billId" to "${mockBill.type}${mockBill.number}-${mockBill.congress}",
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "Mock ingestion failed: ${e.message}",
                    "error" to e.javaClass.simpleName,
                ),
            )
        }

    /**
     * Test endpoint to verify system health and readiness.
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> =
        try {
            val ollamaReady = ollamaService.isServiceReady()
            ResponseEntity.ok(
                mapOf<String, Any>(
                    "status" to "healthy",
                    "message" to "Test endpoints are ready",
                    "ollamaServiceReady" to ollamaReady,
                    "timestamp" to System.currentTimeMillis(),
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.ok(
                mapOf<String, Any>(
                    "status" to "healthy",
                    "message" to "Test endpoints are ready",
                    "ollamaServiceReady" to false,
                    "ollamaError" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis(),
                ),
            )
        }

    /**
     * Test endpoint to check Ollama service status.
     */
    @GetMapping("/ollama-status")
    fun ollamaStatus(): ResponseEntity<Map<String, Any>> =
        runBlocking {
            try {
                val isServiceReady = ollamaService.isServiceReady()
                val isModelAvailable = ollamaService.isModelAvailable()

                ResponseEntity.ok(
                    mapOf(
                        "serviceReady" to isServiceReady,
                        "modelAvailable" to isModelAvailable,
                        "status" to if (isServiceReady && isModelAvailable) "ready" else "initializing",
                        "message" to
                            when {
                                isServiceReady && isModelAvailable -> "Ollama service is ready with model loaded"
                                isServiceReady && !isModelAvailable -> "Ollama service is ready but model is not yet available"
                                else -> "Ollama service is still initializing"
                            },
                        "timestamp" to System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                ResponseEntity.ok(
                    mapOf(
                        "serviceReady" to false,
                        "modelAvailable" to false,
                        "status" to "error",
                        "message" to "Error checking Ollama status: ${e.message}",
                        "timestamp" to System.currentTimeMillis(),
                    ),
                )
            }
        }
}
