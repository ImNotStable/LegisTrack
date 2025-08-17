package com.legistrack.domain.port

import java.time.LocalDate

/**
 * Lightweight domain-facing DTO abstractions hiding external API specifics.
 * Adapter will map external DTOs into these structures before returning.
 */
data class CongressBillSummary(val congress: Int?, val number: String?, val type: String?, val title: String?, val introducedDate: String?)

/** Marker wrappers for collection responses (kept minimal for domain). */
data class CongressBillsPage(val bills: List<CongressBillSummary> = emptyList())

// Additional minimal types can be added as domain needs evolve. For Phase 3 we only require recent bills + bill details.
data class CongressBillDetail(val bill: CongressBillSummary?)

/**
 * Port interface for Congress API operations.
 *
 * Defines the contract for accessing U.S. Congress legislative data
 * without coupling to specific implementation details.
 */
interface CongressPort {
    suspend fun getRecentBills(fromDate: LocalDate, offset: Int = 0, limit: Int = 20): CongressBillsPage
    suspend fun getBillDetails(congress: Int, billType: String, billNumber: String): CongressBillDetail?
    /** Lightweight availability probe (non-throwing). Should return true if API reachable quickly. */
    suspend fun ping(): Boolean = runCatching {
        // Default naive implementation uses a minimal recent bills query; adapters can override with cheaper endpoint.
        getRecentBills(LocalDate.now().minusDays(1), 0, 1).bills.isNotEmpty() || true
    }.getOrDefault(false)
}