package com.legistrack.domain.port

// TODO: Phase 3 - Move external DTOs and implement this interface
/*
import com.legistrack.dto.external.CongressActionsResponse
import com.legistrack.dto.external.CongressAmendment
import com.legistrack.dto.external.CongressAmendmentsResponse
import com.legistrack.dto.external.CongressBill
import com.legistrack.dto.external.CongressBillsResponse
import com.legistrack.dto.external.CongressCosponsorsResponse
import com.legistrack.dto.external.CongressMember
import com.legistrack.dto.external.CongressMembersResponse
import com.legistrack.dto.external.CongressNominationsResponse
import com.legistrack.dto.external.CongressReportsResponse
import com.legistrack.dto.external.CongressSummariesResponse
import com.legistrack.dto.external.CongressTextVersionsResponse
import com.legistrack.dto.external.CongressTreatiesResponse
import java.time.LocalDate

/**
 * Port interface for Congress API operations.
 *
 * Defines the contract for accessing U.S. Congress legislative data
 * without coupling to specific implementation details.
 */
interface CongressPort {
    /**
     * Retrieves recent bills from Congress API.
     *
     * @param fromDate Starting date for bill search
     * @param offset Pagination offset
     * @param limit Number of bills to retrieve
     * @return Response containing bills data
     */
    suspend fun getRecentBills(
        fromDate: LocalDate,
        offset: Int = 0,
        limit: Int = 20,
    ): CongressBillsResponse

    /**
     * Retrieves detailed information for a specific bill.
     *
     * @param congress Congressional session number
     * @param billType Type of bill (HR, S, etc.)
     * @param billNumber Bill number
     * @return Bill details or null if not found
     */
    suspend fun getBillDetails(
        congress: Int,
        billType: String,
        billNumber: String,
    ): CongressBill?

    /**
     * Retrieves cosponsors for a specific bill.
     *
     * @param congress Congressional session number
     * @param billType Type of bill
     * @param billNumber Bill number
     * @return Cosponsors response
     */
    suspend fun getBillCosponsors(
        congress: Int,
        billType: String,
        billNumber: String,
    ): CongressCosponsorsResponse

    /**
     * Retrieves actions taken on a specific bill.
     *
     * @param congress Congressional session number
     * @param billType Type of bill
     * @param billNumber Bill number
     * @return Actions response
     */
    suspend fun getBillActions(
        congress: Int,
        billType: String,
        billNumber: String,
    ): CongressActionsResponse

    /**
     * Retrieves amendments for a congressional session.
     *
     * @param congress Congressional session number
     * @param offset Pagination offset
     * @param limit Number of amendments to retrieve
     * @return Amendments response
     */
    suspend fun getAmendments(
        congress: Int,
        offset: Int = 0,
        limit: Int = 20,
    ): CongressAmendmentsResponse

    /**
     * Retrieves detailed information for a specific amendment.
     *
     * @param congress Congressional session number
     * @param amendmentType Type of amendment
     * @param amendmentNumber Amendment number
     * @return Amendment details or null if not found
     */
    suspend fun getAmendmentDetails(
        congress: Int,
        amendmentType: String,
        amendmentNumber: String,
    ): CongressAmendment?

    /**
     * Retrieves bill summaries.
     *
     * @param congress Congressional session number
     * @param billType Type of bill
     * @param billNumber Bill number
     * @return Summaries response
     */
    suspend fun getBillSummaries(
        congress: Int,
        billType: String,
        billNumber: String,
    ): CongressSummariesResponse

    /**
     * Retrieves members of Congress.
     *
     * @param congress Congressional session number
     * @param chamber Chamber (house/senate) or null for both
     * @param offset Pagination offset
     * @param limit Number of members to retrieve
     * @return Members response
     */
    suspend fun getMembers(
        congress: Int,
        chamber: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): CongressMembersResponse

    /**
     * Retrieves detailed information for a specific member.
     *
     * @param bioguideId Member's bioguide identifier
     * @return Member details or null if not found
     */
    suspend fun getMemberDetails(bioguideId: String): CongressMember?

    /**
     * Retrieves committee reports.
     *
     * @param congress Congressional session number
     * @param reportType Type of report or null for all
     * @param offset Pagination offset
     * @param limit Number of reports to retrieve
     * @return Reports response
     */
    suspend fun getCommitteeReports(
        congress: Int,
        reportType: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): CongressReportsResponse

    /**
     * Retrieves nominations.
     *
     * @param congress Congressional session number
     * @param offset Pagination offset
     * @param limit Number of nominations to retrieve
     * @return Nominations response
     */
    suspend fun getNominations(
        congress: Int,
        offset: Int = 0,
        limit: Int = 20,
    ): CongressNominationsResponse

    /**
     * Retrieves treaties.
     *
     * @param congress Congressional session number
     * @param offset Pagination offset
     * @param limit Number of treaties to retrieve
     * @return Treaties response
     */
    suspend fun getTreaties(
        congress: Int,
        offset: Int = 0,
        limit: Int = 20,
    ): CongressTreatiesResponse

    /**
     * Retrieves text versions for a bill.
     *
     * @param congress Congressional session number
     * @param billType Type of bill
     * @param billNumber Bill number
     * @return Text versions response
     */
    suspend fun getBillTextVersions(
        congress: Int,
        billType: String,
        billNumber: String,
    ): CongressTextVersionsResponse
}
*/