package com.legistrack.service.external

import com.legistrack.dto.external.CongressBill
import com.legistrack.dto.external.CongressBillsResponse
import com.legistrack.dto.external.CongressPagination
import com.legistrack.dto.external.GovInfoCollection
import com.legistrack.dto.external.GovInfoCollectionsResponse
import com.legistrack.dto.external.GovInfoPackage
import com.legistrack.dto.external.GovInfoSearchRequest
import com.legistrack.dto.external.GovInfoSearchResponse
import com.legistrack.dto.external.GovInfoSearchResult
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate

/**
 * Unit tests for external API services.
 *
 * These tests verify that the API services are properly configured
 * and can handle responses correctly.
 */
class ExternalApiServicesTest {
    private lateinit var mockWebClient: WebClient
    private lateinit var mockWebClientRequestUriSpec: WebClient.RequestBodyUriSpec
    private lateinit var mockWebClientGetSpec: WebClient.RequestHeadersUriSpec<*>
    private lateinit var mockWebClientRequestHeadersSpec: WebClient.RequestHeadersSpec<*>
    private lateinit var mockWebClientResponseSpec: WebClient.ResponseSpec

    private lateinit var congressApiService: CongressApiService
    private lateinit var govInfoApiService: GovInfoApiService

    @BeforeEach
    fun setup() {
        mockWebClient = mockk()
        mockWebClientRequestUriSpec = mockk()
        mockWebClientGetSpec = mockk()
        mockWebClientRequestHeadersSpec = mockk()
        mockWebClientResponseSpec = mockk()

        congressApiService =
            CongressApiService(
                webClient = mockWebClient,
                objectMapper = ObjectMapper(),
                apiKey = "test-key",
                baseUrl = "https://api.data.gov/congress/v3",
            )

        govInfoApiService =
            GovInfoApiService(
                webClient = mockWebClient,
                apiKey = "test-key",
                baseUrl = "https://api.govinfo.gov",
            )
    }

    @Test
    fun `CongressApiService can fetch recent bills`() =
        runBlocking {
            // Arrange
            val mockResponse =
                CongressBillsResponse(
                    bills =
                        listOf(
                            CongressBill(
                                congress = 118,
                                number = "1234",
                                type = "hr",
                                title = "Test Bill",
                                introducedDate = "2024-01-01",
                            ),
                        ),
                    pagination = CongressPagination(count = 1),
                )

            every { mockWebClient.get() } returns mockWebClientGetSpec
            every { mockWebClientGetSpec.uri(any<java.net.URI>()) } returns mockWebClientRequestHeadersSpec
            every { mockWebClientRequestHeadersSpec.retrieve() } returns mockWebClientResponseSpec
            every { mockWebClientResponseSpec.bodyToMono(CongressBillsResponse::class.java) } returns Mono.just(mockResponse)

            // Act
            val result =
                assertDoesNotThrow {
                    congressApiService.getRecentBills(LocalDate.now().minusDays(1))
                }

            // Assert
            assertNotNull(result)
            assertEquals(1, result.bills.size)
            assertEquals("Test Bill", result.bills.first().title)
            assertEquals(118, result.bills.first().congress)

            // Verify that the WebClient was called
            verify { mockWebClient.get() }
            verify { mockWebClientGetSpec.uri(any<java.net.URI>()) }
        }

    @Test
    fun `CongressApiService can fetch bill details`() =
        runBlocking {
            // Arrange
            val mockResponse =
                mapOf(
                    "bill" to
                        mapOf(
                            "congress" to 118,
                            "number" to "1234",
                            "type" to "hr",
                            "title" to "Test Bill Details",
                            "introducedDate" to "2024-01-01",
                        ),
                )

            every { mockWebClient.get() } returns mockWebClientGetSpec
            every { mockWebClientGetSpec.uri(any<java.net.URI>()) } returns mockWebClientRequestHeadersSpec
            every { mockWebClientRequestHeadersSpec.retrieve() } returns mockWebClientResponseSpec
            every { mockWebClientResponseSpec.bodyToMono(Map::class.java) } returns Mono.just(mockResponse)

            // Act
            val result =
                assertDoesNotThrow {
                    congressApiService.getBillDetails(118, "hr", "1234")
                }

            // Assert
            assertNotNull(result)
            assertEquals("Test Bill Details", result!!.title)
            assertEquals(118, result.congress)
            assertEquals("hr", result.type)

            // Verify that the WebClient was called correctly
            verify { mockWebClient.get() }
        }

    @Test
    fun `GovInfoApiService can fetch collections`() =
        runBlocking {
            // Arrange
            val mockResponse =
                GovInfoCollectionsResponse(
                    count = 1,
                    collections =
                        listOf(
                            GovInfoCollection(
                                collectionCode = "BILLS",
                                collectionName = "Congressional Bills",
                                packageCount = 12345,
                                granuleCount = 0,
                            ),
                        ),
                )

            every { mockWebClient.get() } returns mockWebClientGetSpec
            every { mockWebClientGetSpec.uri(any<java.net.URI>()) } returns mockWebClientRequestHeadersSpec
            every { mockWebClientRequestHeadersSpec.retrieve() } returns mockWebClientResponseSpec
            every { mockWebClientResponseSpec.bodyToMono(GovInfoCollectionsResponse::class.java) } returns Mono.just(mockResponse)

            // Act
            val result =
                assertDoesNotThrow {
                    govInfoApiService.getCollections()
                }

            // Assert
            assertNotNull(result)
            assertEquals(1, result.collections.size)
            assertEquals("BILLS", result.collections.first().collectionCode)
            assertEquals("Congressional Bills", result.collections.first().collectionName)

            // Verify the interaction
            verify { mockWebClient.get() }
        }

    @Test
    fun `GovInfoApiService can search documents`() =
        runBlocking {
            // Arrange
            val mockResponse =
                GovInfoSearchResponse(
                    count = 1,
                    packages =
                        listOf(
                            GovInfoSearchResult(
                                packageId = "BILLS-118hr5678ih",
                                title = "Search Result Bill",
                                collectionCode = "BILLS",
                                congress = "118",
                                relevancy = "98.5",
                            ),
                        ),
                )

            every { mockWebClient.post() } returns mockWebClientRequestUriSpec
            every { mockWebClientRequestUriSpec.uri(any<java.net.URI>()) } returns mockWebClientRequestUriSpec
            every { mockWebClientRequestUriSpec.bodyValue(any<GovInfoSearchRequest>()) } returns mockWebClientRequestHeadersSpec
            every { mockWebClientRequestHeadersSpec.retrieve() } returns mockWebClientResponseSpec
            every { mockWebClientResponseSpec.bodyToMono(GovInfoSearchResponse::class.java) } returns Mono.just(mockResponse)

            // Act
            val result =
                assertDoesNotThrow {
                    govInfoApiService.searchDocuments("collection:BILLS AND healthcare")
                }

            // Assert
            assertNotNull(result)
            assertEquals(1, result.packages.size)
            assertEquals("Search Result Bill", result.packages.first().title)
            assertEquals("98.5", result.packages.first().relevancy)

            // Verify the POST call was made
            verify { mockWebClient.post() }
        }

    @Test
    fun `services handle errors gracefully`() =
        runBlocking {
            // Arrange - Mock a network error
            every { mockWebClient.get() } returns mockWebClientGetSpec
            every { mockWebClientGetSpec.uri(any<java.net.URI>()) } returns mockWebClientRequestHeadersSpec
            every { mockWebClientRequestHeadersSpec.retrieve() } returns mockWebClientResponseSpec
            every { mockWebClientResponseSpec.bodyToMono(CongressBillsResponse::class.java) } throws RuntimeException("Network error")

            // Act & Assert - Should not throw, but return empty response
            val result =
                assertDoesNotThrow {
                    congressApiService.getRecentBills(LocalDate.now())
                }

            assertNotNull(result)
            assertEquals(0, result.bills.size) // Empty response on error

            // Verify that the error was handled and didn't propagate
            verify { mockWebClient.get() }
        }

    @Test
    fun `DTOs are properly structured`() {
        // Test that our DTOs can be instantiated correctly
        val congressBill =
            CongressBill(
                congress = 118,
                number = "1234",
                type = "hr",
                title = "Test Bill",
                introducedDate = "2024-01-01",
            )

        assertEquals(118, congressBill.congress)
        assertEquals("Test Bill", congressBill.title)

        val govInfoPackage =
            GovInfoPackage(
                packageId = "BILLS-118hr1234ih",
                title = "Test Package",
                collectionCode = "BILLS",
            )

        assertEquals("BILLS-118hr1234ih", govInfoPackage.packageId)
        assertEquals("BILLS", govInfoPackage.collectionCode)
    }
}
