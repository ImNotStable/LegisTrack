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

/**
 * Service for interacting with GovInfo.gov API.
 * 
 * Provides methods for accessing government documents, metadata, and content
 * from all three branches of the Federal Government as published on govinfo.gov.
 */
@Service
class GovInfoApiService(
    private val webClient: WebClient,
    @Value("\${app.govinfo.api.key}") private val apiKey: String,
    @Value("\${app.govinfo.api.base-url}") private val baseUrl: String
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(GovInfoApiService::class.java)
        private const val RETRY_ATTEMPTS = 3L
        private const val RETRY_DELAY_SECONDS = 2L
        private const val DEFAULT_PAGE_SIZE = 20
        private const val DEFAULT_OFFSET_MARK = "*"
    }
    
    /**
     * Get all available collections from GovInfo.
     * 
     * @param pageSize Number of collections to return per page
     * @param offsetMark Pagination offset marker
     * @return Response containing collections metadata
     */
    @Cacheable("govinfo-collections", key = "#pageSize + '_' + #offsetMark")
    suspend fun getCollections(pageSize: Int = DEFAULT_PAGE_SIZE, offsetMark: String = DEFAULT_OFFSET_MARK): GovInfoCollectionsResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/collections")
            .queryParam("api_key", apiKey)
            .queryParam("pageSize", pageSize)
            .queryParam("offsetMark", offsetMark)
            .build()
            .toUri()
        
        logger.debug("Fetching collections from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoCollectionsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: GovInfoCollectionsResponse()
        } catch (e: Exception) {
            logger.error("Error fetching collections from GovInfo API", e)
            GovInfoCollectionsResponse()
        }
    }
    
    /**
     * Get packages from a specific collection based on last modified date.
     * 
     * @param collectionCode Collection code (e.g., "BILLS", "FR", "CFR")
     * @param lastModifiedStartDate Start date for last modified filter
     * @param lastModifiedEndDate End date for last modified filter
     * @param pageSize Number of packages to return per page
     * @param offsetMark Pagination offset marker
     * @return Response containing packages from the collection
     */
    @Cacheable("govinfo-packages", key = "#collectionCode + '_' + #lastModifiedStartDate + '_' + #lastModifiedEndDate + '_' + #pageSize + '_' + #offsetMark")
    suspend fun getPackages(
        collectionCode: String,
        lastModifiedStartDate: LocalDate,
        lastModifiedEndDate: LocalDate? = null,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offsetMark: String = DEFAULT_OFFSET_MARK
    ): GovInfoPackagesResponse {
        val startDateStr = lastModifiedStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDateStr = lastModifiedEndDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: startDateStr
        
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/collections/{collectionCode}/{startDate}/{endDate}")
            .queryParam("api_key", apiKey)
            .queryParam("pageSize", pageSize)
            .queryParam("offsetMark", offsetMark)
            .buildAndExpand(collectionCode, startDateStr, endDateStr)
            .toUri()
        
        logger.debug("Fetching packages from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoPackagesResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: GovInfoPackagesResponse()
        } catch (e: Exception) {
            logger.error("Error fetching packages from GovInfo API", e)
            GovInfoPackagesResponse()
        }
    }
    
    /**
     * Get detailed information about a specific package.
     * 
     * @param packageId Unique package identifier
     * @return Package details including metadata and download links
     */
    @Cacheable("govinfo-package-details", key = "#packageId")
    suspend fun getPackageDetails(packageId: String): GovInfoPackageDetailsResponse? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/packages/{packageId}/summary")
            .queryParam("api_key", apiKey)
            .buildAndExpand(packageId)
            .toUri()
        
        logger.debug("Fetching package details from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoPackageDetailsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block()
        } catch (e: Exception) {
            logger.error("Error fetching package details from GovInfo API", e)
            null
        }
    }
    
    /**
     * Get granules (sub-documents) for a specific package.
     * 
     * @param packageId Package identifier
     * @param pageSize Number of granules to return per page
     * @param offsetMark Pagination offset marker
     * @return Response containing granules for the package
     */
    @Cacheable("govinfo-granules", key = "#packageId + '_' + #pageSize + '_' + #offsetMark")
    suspend fun getPackageGranules(
        packageId: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offsetMark: String = DEFAULT_OFFSET_MARK
    ): GovInfoGranulesResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/packages/{packageId}/granules")
            .queryParam("api_key", apiKey)
            .queryParam("pageSize", pageSize)
            .queryParam("offsetMark", offsetMark)
            .buildAndExpand(packageId)
            .toUri()
        
        logger.debug("Fetching package granules from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoGranulesResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: GovInfoGranulesResponse()
        } catch (e: Exception) {
            logger.error("Error fetching package granules from GovInfo API", e)
            GovInfoGranulesResponse()
        }
    }
    
    /**
     * Get detailed information about a specific granule.
     * 
     * @param granuleId Unique granule identifier
     * @return Granule details including metadata and download links
     */
    @Cacheable("govinfo-granule-details", key = "#granuleId")
    suspend fun getGranuleDetails(granuleId: String): GovInfoGranuleDetailsResponse? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/packages/{granuleId}/summary")
            .queryParam("api_key", apiKey)
            .buildAndExpand(granuleId)
            .toUri()
        
        logger.debug("Fetching granule details from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoGranuleDetailsResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block()
        } catch (e: Exception) {
            logger.error("Error fetching granule details from GovInfo API", e)
            null
        }
    }
    
    /**
     * Get packages based on official publication date.
     * 
     * @param dateIssued Publication date to filter by
     * @param collectionCode Optional collection filter
     * @param docClass Optional document class filter
     * @param pageSize Number of packages to return per page
     * @param offsetMark Pagination offset marker
     * @return Response containing published packages
     */
    @Cacheable("govinfo-published", key = "#dateIssued + '_' + #collectionCode + '_' + #docClass + '_' + #pageSize + '_' + #offsetMark")
    suspend fun getPublishedPackages(
        dateIssued: LocalDate,
        collectionCode: String? = null,
        docClass: String? = null,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offsetMark: String = DEFAULT_OFFSET_MARK
    ): GovInfoPublishedResponse {
        val dateStr = dateIssued.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/published/{dateIssued}")
            .queryParam("api_key", apiKey)
            .queryParam("pageSize", pageSize)
            .queryParam("offsetMark", offsetMark)
        
        if (collectionCode != null) {
            uriBuilder.queryParam("collection", collectionCode)
        }
        if (docClass != null) {
            uriBuilder.queryParam("docClass", docClass)
        }
        
        val uri = uriBuilder.buildAndExpand(dateStr).toUri()
        
        logger.debug("Fetching published packages from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoPublishedResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: GovInfoPublishedResponse()
        } catch (e: Exception) {
            logger.error("Error fetching published packages from GovInfo API", e)
            GovInfoPublishedResponse()
        }
    }
    
    /**
     * Get related documents for a given access ID.
     * 
     * @param accessId Package or granule ID
     * @param collectionCode Optional collection filter for relationships
     * @return Response containing related document relationships
     */
    @Cacheable("govinfo-related", key = "#accessId + '_' + #collectionCode")
    suspend fun getRelatedDocuments(accessId: String, collectionCode: String? = null): GovInfoRelatedResponse {
        val uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/related/{accessId}")
            .queryParam("api_key", apiKey)
        
        if (collectionCode != null) {
            uriBuilder.path("/{collectionCode}")
        }
        
        val uri = if (collectionCode != null) {
            uriBuilder.buildAndExpand(accessId, collectionCode).toUri()
        } else {
            uriBuilder.buildAndExpand(accessId).toUri()
        }
        
        logger.debug("Fetching related documents from GovInfo API: {}", uri)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GovInfoRelatedResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: GovInfoRelatedResponse()
        } catch (e: Exception) {
            logger.error("Error fetching related documents from GovInfo API", e)
            GovInfoRelatedResponse()
        }
    }
    
    /**
     * Search GovInfo documents using query and field operators.
     * 
     * @param query Search query string
     * @param pageSize Number of results to return per page
     * @param offsetMark Pagination offset marker
     * @param sortField Field to sort by (relevancy, publishdate, etc.)
     * @param sortOrder Sort order (ASC or DESC)
     * @param historical Include historical documents
     * @return Search results response
     */
    @Cacheable("govinfo-search", key = "#query + '_' + #pageSize + '_' + #offsetMark + '_' + #sortField + '_' + #sortOrder")
    suspend fun searchDocuments(
        query: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offsetMark: String = DEFAULT_OFFSET_MARK,
        sortField: String = "relevancy",
        sortOrder: String = "DESC",
        historical: Boolean = true
    ): GovInfoSearchResponse {
        val searchRequest = GovInfoSearchRequest(
            query = query,
            pageSize = pageSize,
            offsetMark = offsetMark,
            sorts = listOf(GovInfoSort(sortField, sortOrder)),
            historical = historical,
            resultLevel = "default"
        )
        
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/search")
            .queryParam("api_key", apiKey)
            .build()
            .toUri()
        
        logger.debug("Searching documents in GovInfo API: query={}, pageSize={}", query, pageSize)
        
        return try {
            webClient.post()
                .uri(uri)
                .bodyValue(searchRequest)
                .retrieve()
                .bodyToMono(GovInfoSearchResponse::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block() ?: GovInfoSearchResponse()
        } catch (e: Exception) {
            logger.error("Error searching documents in GovInfo API", e)
            GovInfoSearchResponse()
        }
    }
    
    /**
     * Get bills from the BILLS collection.
     * 
     * @param congress Congress number
     * @param billType Bill type (hr, s, hjres, sjres, hconres, sconres, hres, sres)
     * @param lastModifiedStartDate Start date for last modified filter
     * @param lastModifiedEndDate End date for last modified filter
     * @param pageSize Number of bills to return per page
     * @param offsetMark Pagination offset marker
     * @return Response containing bills from the BILLS collection
     */
    @Cacheable("govinfo-bills", key = "#congress + '_' + #billType + '_' + #lastModifiedStartDate + '_' + #lastModifiedEndDate + '_' + #pageSize + '_' + #offsetMark")
    suspend fun getBills(
        congress: Int? = null,
        billType: String? = null,
        lastModifiedStartDate: LocalDate,
        lastModifiedEndDate: LocalDate? = null,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offsetMark: String = DEFAULT_OFFSET_MARK
    ): GovInfoPackagesResponse {
        var response = getPackages("BILLS", lastModifiedStartDate, lastModifiedEndDate, pageSize, offsetMark)
        
        // Filter by congress and billType if specified
        if (congress != null || billType != null) {
            val filteredPackages = response.packages.filter { pkg ->
                val matchesCongress = congress == null || pkg.congress == congress.toString()
                val matchesBillType = billType == null || pkg.docClass?.equals(billType, ignoreCase = true) == true
                matchesCongress && matchesBillType
            }
            response = response.copy(
                packages = filteredPackages,
                count = filteredPackages.size
            )
        }
        
        return response
    }
    
    /**
     * Get bill status details from the BILLSTATUS collection.
     * 
     * @param packageId Bill status package ID
     * @return Bill status details or null if not found
     */
    @Cacheable("govinfo-bill-status", key = "#packageId")
    suspend fun getBillStatus(packageId: String): GovInfoBillStatus? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/packages/{packageId}/xml")
            .queryParam("api_key", apiKey)
            .buildAndExpand(packageId)
            .toUri()
        
        logger.debug("Fetching bill status from GovInfo API: {}", uri)
        
        return try {
            // Note: This would typically parse XML response to extract bill status
            // For now, returning null as XML parsing would require additional setup
            logger.debug("Bill status XML parsing not implemented yet for packageId: {}", packageId)
            null
        } catch (e: Exception) {
            logger.error("Error fetching bill status from GovInfo API", e)
            null
        }
    }
    
    /**
     * Download document content in specified format.
     * 
     * @param packageId Package identifier
     * @param format Content format (txt, xml, pdf, zip)
     * @return Content as byte array or null if not available
     */
    suspend fun downloadContent(packageId: String, format: String): ByteArray? {
        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/packages/{packageId}/{format}")
            .queryParam("api_key", apiKey)
            .buildAndExpand(packageId, format)
            .toUri()
        
        logger.debug("Downloading content from GovInfo API: {}, format: {}", packageId, format)
        
        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                .block()
        } catch (e: Exception) {
            logger.error("Error downloading content from GovInfo API", e)
            null
        }
    }
}
