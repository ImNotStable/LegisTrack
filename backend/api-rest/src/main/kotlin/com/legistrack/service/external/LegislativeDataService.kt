package com.legistrack.service.external

import com.legistrack.domain.port.CongressBillDetail
import com.legistrack.domain.port.CongressBillSummary
import com.legistrack.domain.port.CongressPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LegislativeDataService(private val congressPort: CongressPort) {
    private val logger = LoggerFactory.getLogger(LegislativeDataService::class.java)

    suspend fun getBillDetails(congress: Int, billType: String, billNumber: String): BillDetails? = try {
        logger.debug("Fetching bill details for $billType$billNumber in Congress $congress")
        val congressBillDetail: CongressBillDetail? = congressPort.getBillDetails(congress, billType, billNumber)
        val summary = congressBillDetail?.bill ?: return null.also { logger.warn("No bill found in Congress.gov for $billType$billNumber") }
        BillDetails(congressBill = summary, fullText = null)
    } catch (e: Exception) {
        logger.error("Failed to fetch bill details for $billType$billNumber: ${e.message}", e)
        null
    }

    suspend fun searchBills(query: String, limit: Int = 20): List<BillSearchResult> = try {
        logger.debug("Searching bills with query: $query, limit: $limit")
        val results = mutableListOf<BillSearchResult>()
        val congressResponse = congressPort.getRecentBills(LocalDate.now().minusMonths(1), 0, limit)
        congressResponse.bills.forEach { bill ->
            if (matchesQuery(bill, query)) {
                results.add(
                    BillSearchResult(
                        billId = "${bill.type ?: ""}${bill.number ?: ""}-${bill.congress ?: 0}",
                        title = bill.title ?: "Unknown Title",
                        introducedDate = bill.introducedDate,
                        congress = bill.congress ?: 0,
                        type = bill.type ?: "UNKNOWN",
                        number = bill.number ?: "0",
                        primarySponsor = null,
                        summary = null,
                        source = "Congress.gov",
                    ),
                )
            }
        }
        results.take(limit)
    } catch (e: Exception) {
        logger.error("Failed to search bills with query '$query': ${e.message}", e)
        emptyList()
    }

    private fun matchesQuery(bill: CongressBillSummary, query: String): Boolean {
        val lowerQuery = query.lowercase()
        return bill.title?.lowercase()?.contains(lowerQuery) == true
    }
}

data class BillDetails(val congressBill: CongressBillSummary, val fullText: String? = null)

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
