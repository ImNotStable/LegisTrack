package com.legistrack.service.external

import com.legistrack.dto.external.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import reactor.util.retry.Retry
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class CongressApiService(
    private val webClient: WebClient,
    @Value("\${app.congress.api.key}") private val apiKey: String,
    @Value("\${app.congress.api.base-url}") private val baseUrl: String
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(CongressApiService::class.java)
        private const val RETRY_ATTEMPTS = 3L
        private const val RETRY_DELAY_SECONDS = 2L
    }
    
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
                .bodyToMono(CongressBillsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressBillsResponse()
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
                .bodyToMono(Map::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() as? Map<String, Any>
            
            @Suppress("UNCHECKED_CAST")
            val billData = response?.get("bill") as? Map<String, Any>
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
                .bodyToMono(CongressCosponsorsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressCosponsorsResponse()
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
                .bodyToMono(CongressActionsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressActionsResponse()
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
    
    // Amendment endpoints
    @Cacheable("congress-amendments", key = "#congress + '_' + #offset + '_' + #limit")
    suspend fun getAmendments(congress: Int, offset: Int = 0, limit: Int = 20): CongressAmendmentsResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/amendment/{congress}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
            .buildAndExpand(congress)
            .toUri()
        
        logger.debug("Fetching amendments from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressAmendmentsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressAmendmentsResponse()
        } catch (e: Exception) {
            logger.error("Error fetching amendments from Congress API", e)
            CongressAmendmentsResponse()
        }
    }
    
    @Cacheable("congress-amendment-details", key = "#congress + '_' + #amendmentType + '_' + #amendmentNumber")
    suspend fun getAmendmentDetails(congress: Int, amendmentType: String, amendmentNumber: String): CongressAmendment? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/amendment/{congress}/{type}/{number}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .buildAndExpand(congress, amendmentType.lowercase(), amendmentNumber)
            .toUri()
        
        logger.debug("Fetching amendment details from Congress API: {}", uri)
        
        return try {
            val response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() as? Map<String, Any>
            
            @Suppress("UNCHECKED_CAST")
            val amendmentData = response?.get("amendment") as? Map<String, Any>
            if (amendmentData != null) {
                parseCongressAmendment(amendmentData)
            } else null
        } catch (e: Exception) {
            logger.error("Error fetching amendment details from Congress API", e)
            null
        }
    }
    
    // Summary endpoints
    @Cacheable("congress-summaries", key = "#congress + '_' + #billType + '_' + #billNumber")
    suspend fun getBillSummaries(congress: Int, billType: String, billNumber: String): CongressSummariesResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/bill/{congress}/{type}/{number}/summaries")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .buildAndExpand(congress, billType.lowercase(), billNumber)
            .toUri()
        
        logger.debug("Fetching bill summaries from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressSummariesResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressSummariesResponse()
        } catch (e: Exception) {
            logger.error("Error fetching summaries from Congress API", e)
            CongressSummariesResponse()
        }
    }
    
    // Member endpoints
    @Cacheable("congress-members", key = "#congress + '_' + #chamber + '_' + #offset + '_' + #limit")
    suspend fun getMembers(congress: Int, chamber: String? = null, offset: Int = 0, limit: Int = 20): CongressMembersResponse {
        val uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/member/{congress}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
        
        if (chamber != null) {
            uriBuilder.queryParam("currentMember", "true")
        }
        
        val uri = uriBuilder.buildAndExpand(congress).toUri()
        
        logger.debug("Fetching members from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressMembersResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressMembersResponse()
        } catch (e: Exception) {
            logger.error("Error fetching members from Congress API", e)
            CongressMembersResponse()
        }
    }
    
    @Cacheable("congress-member-details", key = "#bioguideId")
    suspend fun getMemberDetails(bioguideId: String): CongressMember? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/member/{bioguideId}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .buildAndExpand(bioguideId)
            .toUri()
        
        logger.debug("Fetching member details from Congress API: {}", uri)
        
        return try {
            val response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() as? Map<String, Any>
            
            @Suppress("UNCHECKED_CAST")
            val memberData = response?.get("member") as? Map<String, Any>
            if (memberData != null) {
                parseCongressMember(memberData)
            } else null
        } catch (e: Exception) {
            logger.error("Error fetching member details from Congress API", e)
            null
        }
    }
    
    // Committee Report endpoints
    @Cacheable("congress-reports", key = "#congress + '_' + #reportType + '_' + #offset + '_' + #limit")
    suspend fun getCommitteeReports(congress: Int, reportType: String? = null, offset: Int = 0, limit: Int = 20): CongressReportsResponse {
        val uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/committee-report/{congress}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
        
        if (reportType != null) {
            uriBuilder.queryParam("type", reportType)
        }
        
        val uri = uriBuilder.buildAndExpand(congress).toUri()
        
        logger.debug("Fetching committee reports from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressReportsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressReportsResponse()
        } catch (e: Exception) {
            logger.error("Error fetching committee reports from Congress API", e)
            CongressReportsResponse()
        }
    }
    
    // Nomination endpoints
    @Cacheable("congress-nominations", key = "#congress + '_' + #offset + '_' + #limit")
    suspend fun getNominations(congress: Int, offset: Int = 0, limit: Int = 20): CongressNominationsResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/nomination/{congress}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
            .buildAndExpand(congress)
            .toUri()
        
        logger.debug("Fetching nominations from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressNominationsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressNominationsResponse()
        } catch (e: Exception) {
            logger.error("Error fetching nominations from Congress API", e)
            CongressNominationsResponse()
        }
    }
    
    // Treaty endpoints
    @Cacheable("congress-treaties", key = "#congress + '_' + #offset + '_' + #limit")
    suspend fun getTreaties(congress: Int, offset: Int = 0, limit: Int = 20): CongressTreatiesResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/treaty/{congress}")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .queryParam("limit", limit)
            .queryParam("offset", offset)
            .buildAndExpand(congress)
            .toUri()
        
        logger.debug("Fetching treaties from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressTreatiesResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressTreatiesResponse()
        } catch (e: Exception) {
            logger.error("Error fetching treaties from Congress API", e)
            CongressTreatiesResponse()
        }
    }
    
    // Text Version endpoints
    @Cacheable("congress-text-versions", key = "#congress + '_' + #billType + '_' + #billNumber")
    suspend fun getBillTextVersions(congress: Int, billType: String, billNumber: String): CongressTextVersionsResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/bill/{congress}/{type}/{number}/text")
            .queryParam("api_key", apiKey)
            .queryParam("format", "json")
            .buildAndExpand(congress, billType.lowercase(), billNumber)
            .toUri()
        
        logger.debug("Fetching bill text versions from Congress API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(CongressTextVersionsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: CongressTextVersionsResponse()
        } catch (e: Exception) {
            logger.error("Error fetching text versions from Congress API", e)
            CongressTextVersionsResponse()
        }
    }
    
    // Helper parsing methods
    private fun parseCongressAmendment(amendmentData: Map<String, Any>): CongressAmendment {
        return CongressAmendment(
            congress = amendmentData["congress"] as? Int,
            number = amendmentData["number"] as? String,
            type = amendmentData["type"] as? String,
            purpose = amendmentData["purpose"] as? String,
            description = amendmentData["description"] as? String,
            submittedDate = amendmentData["submittedDate"] as? String
        )
    }
    
    private fun parseCongressMember(memberData: Map<String, Any>): CongressMember {
        return CongressMember(
            bioguideId = memberData["bioguideId"] as? String,
            firstName = memberData["firstName"] as? String,
            lastName = memberData["lastName"] as? String,
            middleName = memberData["middleName"] as? String,
            party = memberData["party"] as? String,
            state = memberData["state"] as? String,
            district = memberData["district"] as? String
        )
    }
}
