package com.legistrack.service.external

import com.legistrack.dto.external.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class CongressApiService(
    private val webClient: WebClient,
    @Value("\${app.congress.api.key}") private val apiKey: String,
    @Value("\${app.congress.api.base-url}") private val baseUrl: String
) {
    
    private val logger = LoggerFactory.getLogger(CongressApiService::class.java)
    
    @Cacheable("congress-bills", key = "#fromDate.toString() + '_' + #offset + '_' + #limit")
    suspend fun getRecentBills(fromDate: LocalDate, offset: Int = 0, limit: Int = 20): CongressBillsResponse {
        val dateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/bill")
            .queryParam("api_key", apiKey)
            .queryParam("fromDateTime", "${dateStr}T00:00:00Z")
            .queryParam("sort", "latestAction.actionDate+desc")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
            .queryParam("format", "json")
            .build()
            .toUri()
        
        logger.debug("Fetching bills from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .awaitBody<CongressBillsResponse>()
        } catch (e: Exception) {
            logger.error("Error fetching bills from Congress API", e)
            CongressBillsResponse()
        }
    }
    
    @Cacheable("congress-bill-details", key = "#congress + '_' + #billType + '_' + #billNumber")
    suspend fun getBillDetails(congress: Int, billType: String, billNumber: String): CongressBill? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/bill/{congress}/{type}/{number}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .buildAndExpand(congress, billType.lowercase(), billNumber)
            .toUri()
        
        logger.debug("Fetching bill details from Congress API: {}", uri)
        
        return try {
            val response = webClient.get()
                .uri(uri)
                .retrieve()
                .awaitBody<Map<String, Any>>()
            
            @Suppress("UNCHECKED_CAST")
            val billData = response["bill"] as? Map<String, Any>
            if (billData != null) {
                // Convert to CongressBill object manually or use ObjectMapper
                parseCongressBill(billData)
            } else null
        } catch (e: Exception) {
            logger.error("Error fetching bill details from Congress API", e)
            null
        }
    }
    
    @Cacheable("congress-cosponsors", key = "#congress + '_' + #billType + '_' + #billNumber")
    suspend fun getBillCosponsors(congress: Int, billType: String, billNumber: String): CongressCosponsorsResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/bill/{congress}/{type}/{number}/cosponsors")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", 250)
            .buildAndExpand(congress, billType.lowercase(), billNumber)
            .toUri()
        
        logger.debug("Fetching bill cosponsors from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .awaitBody<CongressCosponsorsResponse>()
        } catch (e: Exception) {
            logger.error("Error fetching cosponsors from Congress API", e)
            CongressCosponsorsResponse()
        }
    }
    
    @Cacheable("congress-actions", key = "#congress + '_' + #billType + '_' + #billNumber")
    suspend fun getBillActions(congress: Int, billType: String, billNumber: String): CongressActionsResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/bill/{congress}/{type}/{number}/actions")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", 250)
            .buildAndExpand(congress, billType.lowercase(), billNumber)
            .toUri()
        
        logger.debug("Fetching bill actions from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .awaitBody<CongressActionsResponse>()
        } catch (e: Exception) {
            logger.error("Error fetching actions from Congress API", e)
            CongressActionsResponse()
        }
    }
    
    private fun parseCongressBill(billData: Map<String, Any>): CongressBill {
        // Simple parsing - in production, you might want to use ObjectMapper
        return CongressBill(
            congress = billData["congress"] as? Int,
            number = billData["number"] as? String,
            type = billData["type"] as? String,
            title = billData["title"] as? String,
            introducedDate = billData["introducedDate"] as? String
            // Add more fields as needed
        )
    }
}
