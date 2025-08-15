package com.legistrack.service.external

import com.legistrack.dto.external.CongressBill
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Service for aggregating and processing legislative data from multiple sources.
 * Provides unified access to bill information, summaries, and metadata.
 */
@Service
class LegislativeDataService(
    private val congressApiService: CongressApiService,
    private val govInfoApiService: GovInfoApiService,
) {
    private val logger = LoggerFactory.getLogger(LegislativeDataService::class.java)

    /**
     * Get comprehensive bill information by combining data from Congress.gov and GovInfo.gov
     */
    suspend fun getBillDetails(
        congress: Int,
        billType: String,
        billNumber: String,
    ): BillDetails? {
        return try {
            logger.debug("Fetching comprehensive bill details for $billType$billNumber in Congress $congress")

            // Get basic bill info from Congress.gov
            val congressBill = congressApiService.getBillDetails(congress, billType, billNumber)
            if (congressBill == null) {
                logger.warn("No bill found in Congress.gov for $billType$billNumber")
                return null
            }

            // Get additional text content from GovInfo if available
            val govInfoText = getGovInfoText(congress, billType, billNumber)

            BillDetails(
                congressBill = congressBill,
                fullText = govInfoText,
            )
        } catch (e: Exception) {
            logger.error("Failed to fetch comprehensive bill details for $billType$billNumber: ${e.message}", e)
            null
        }
    }

    /**
     * Search for bills across both APIs with enhanced metadata
     */
    suspend fun searchBills(
        query: String,
        limit: Int = 20,
    ): List<BillSearchResult> =
        try {
            logger.debug("Searching bills with query: $query, limit: $limit")

            val results = mutableListOf<BillSearchResult>()

            // Search Congress.gov for bills
            val congressResponse = congressApiService.getRecentBills(LocalDate.now().minusMonths(1), 0, limit)
            congressResponse.bills.forEach { bill ->
                if (matchesQuery(bill, query)) {
                    results.add(
                        BillSearchResult(
                            billId = "${bill.type}${bill.number}-${bill.congress}",
                            title = bill.title ?: "Unknown Title",
                            introducedDate = bill.introducedDate,
                            congress = bill.congress ?: 0,
                            type = bill.type ?: "UNKNOWN",
                            number = bill.number ?: "0",
                            primarySponsor =
                                bill.sponsors.firstOrNull()?.let { sponsor ->
                                    "${sponsor.firstName} ${sponsor.lastName} (${sponsor.party}-${sponsor.state})"
                                },
                            summary =
                                bill.summaries
                                    .firstOrNull()
                                    ?.text
                                    ?.take(200),
                            source = "Congress.gov",
                        ),
                    )
                }
            }

            // Search GovInfo for additional context (if needed)
            if (results.size < limit) {
                val govInfoResponse = govInfoApiService.searchDocuments(query, limit - results.size)
                govInfoResponse.packages.forEach { doc ->
                    if (doc.docClass == "bills" && !results.any { it.billId == doc.packageId }) {
                        results.add(
                            BillSearchResult(
                                billId = doc.packageId ?: "unknown",
                                title = doc.title ?: "Unknown Title",
                                introducedDate = doc.dateIssued,
                                congress = extractCongressFromPackageId(doc.packageId),
                                type = extractTypeFromPackageId(doc.packageId),
                                number = extractNumberFromPackageId(doc.packageId),
                                primarySponsor = null,
                                summary = doc.title?.take(200),
                                source = "GovInfo.gov",
                            ),
                        )
                    }
                }
            }

            results.take(limit)
        } catch (e: Exception) {
            logger.error("Failed to search bills with query '$query': ${e.message}", e)
            emptyList()
        }

    /**
     * Get bill text content from GovInfo.gov
     */
    private suspend fun getGovInfoText(
        congress: Int,
        billType: String,
        billNumber: String,
    ): String? =
        try {
            val packageId = "${billType.lowercase()}$billNumber-$congress"
            val searchResponse = govInfoApiService.searchDocuments(packageId, 1)
            val pkg = searchResponse.packages.firstOrNull()
            pkg?.let { p ->
                val details = govInfoApiService.getPackageDetails(p.packageId ?: return@let null)
                val txtLink = details?.download?.txtLink
                if (!txtLink.isNullOrBlank()) {
                    val id = details.packageId ?: return@let null
                    val bytes = govInfoApiService.downloadContent(id, "txt")
                    bytes?.toString(Charsets.UTF_8)
                } else {
                    details?.title
                }
            }
        } catch (e: Exception) {
            logger.debug("Failed to get GovInfo text for $billType$billNumber: ${e.message}")
            null
        }

    /**
     * Check if a bill matches the search query
     */
    private fun matchesQuery(
        bill: CongressBill,
        query: String,
    ): Boolean {
        val lowerQuery = query.lowercase()
        return (bill.title?.lowercase()?.contains(lowerQuery) == true) ||
            bill.sponsors.any { sponsor ->
                "${sponsor.firstName ?: ""} ${sponsor.lastName ?: ""}".lowercase().contains(lowerQuery)
            } ||
            bill.summaries.any { summary ->
                summary.text?.lowercase()?.contains(lowerQuery) == true
            }
    }

    /**
     * Extract congress number from GovInfo package ID
     */
    private fun extractCongressFromPackageId(packageId: String?): Int =
        packageId?.let { id ->
            val regex = """(\d+)$""".toRegex()
            regex
                .find(id)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        } ?: 0

    /**
     * Extract bill type from GovInfo package ID
     */
    private fun extractTypeFromPackageId(packageId: String?): String =
        packageId?.let { id ->
            val regex = """([a-zA-Z]+)\d+""".toRegex()
            regex
                .find(id)
                ?.groupValues
                ?.get(1)
                ?.uppercase()
        } ?: "UNKNOWN"

    /**
     * Extract bill number from GovInfo package ID
     */
    private fun extractNumberFromPackageId(packageId: String?): String =
        packageId?.let { id ->
            val regex = """[a-zA-Z]+(\d+)-\d+""".toRegex()
            regex.find(id)?.groupValues?.get(1)
        } ?: "0"
}

/**
 * Comprehensive bill details combining data from multiple sources
 */
data class BillDetails(
    val congressBill: CongressBill,
    val fullText: String? = null,
)

/**
 * Search result for bill searches across multiple APIs
 */
data class BillSearchResult(
    val billId: String,
    val title: String,
    val introducedDate: String?,
    val congress: Int,
    val type: String,
    val number: String,
    val primarySponsor: String? = null,
    val summary: String? = null,
    val source: String,
)
