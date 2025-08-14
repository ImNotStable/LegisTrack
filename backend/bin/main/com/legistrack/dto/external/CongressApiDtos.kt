package com.legistrack.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// Congress.gov API DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressBillsResponse(
    @JsonProperty("bills") val bills: List<CongressBill> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressBill(
    @JsonProperty("congress") val congress: Int? = null,
    @JsonProperty("number") val number: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("introducedDate") val introducedDate: String? = null,
    @JsonProperty("sponsors") val sponsors: List<CongressSponsor> = emptyList(),
    @JsonProperty("cosponsors") val cosponsors: CongressCosponsors? = null,
    @JsonProperty("actions") val actions: CongressActions? = null,
    @JsonProperty("summaries") val summaries: List<CongressSummary> = emptyList(),
    @JsonProperty("textVersions") val textVersions: CongressTextVersions? = null,
    @JsonProperty("latestAction") val latestAction: CongressAction? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSponsor(
    @JsonProperty("bioguideId") val bioguideId: String? = null,
    @JsonProperty("firstName") val firstName: String? = null,
    @JsonProperty("lastName") val lastName: String? = null,
    @JsonProperty("party") val party: String? = null,
    @JsonProperty("state") val state: String? = null,
    @JsonProperty("district") val district: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCosponsors(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("countIncludingWithdrawnCosponsors") val countIncludingWithdrawnCosponsors: Int = 0,
    @JsonProperty("url") val url: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressActions(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("url") val url: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressAction(
    @JsonProperty("actionDate") val actionDate: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("actionCode") val actionCode: String? = null,
    @JsonProperty("sourceSystem") val sourceSystem: CongressSourceSystem? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSourceSystem(
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("name") val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSummary(
    @JsonProperty("actionDate") val actionDate: String? = null,
    @JsonProperty("actionDesc") val actionDesc: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("updateDate") val updateDate: String? = null,
    @JsonProperty("versionCode") val versionCode: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTextVersions(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("url") val url: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressPagination(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("next") val next: String? = null,
    @JsonProperty("prev") val prev: String? = null,
)

// Detailed sponsor response
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCosponsorsResponse(
    @JsonProperty("cosponsors") val cosponsors: List<CongressCosponsor> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCosponsor(
    @JsonProperty("bioguideId") val bioguideId: String? = null,
    @JsonProperty("firstName") val firstName: String? = null,
    @JsonProperty("lastName") val lastName: String? = null,
    @JsonProperty("party") val party: String? = null,
    @JsonProperty("state") val state: String? = null,
    @JsonProperty("district") val district: String? = null,
    @JsonProperty("sponsorshipDate") val sponsorshipDate: String? = null,
    @JsonProperty("sponsorshipWithdrawnDate") val sponsorshipWithdrawnDate: String? = null,
)

// Detailed actions response
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressActionsResponse(
    @JsonProperty("actions") val actions: List<CongressDetailedAction> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressDetailedAction(
    @JsonProperty("actionDate") val actionDate: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("actionCode") val actionCode: String? = null,
    @JsonProperty("chamber") val chamber: String? = null,
    @JsonProperty("sourceSystem") val sourceSystem: CongressSourceSystem? = null,
)

// Amendments DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressAmendmentsResponse(
    @JsonProperty("amendments") val amendments: List<CongressAmendment> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressAmendment(
    @JsonProperty("congress") val congress: Int? = null,
    @JsonProperty("number") val number: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("purpose") val purpose: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("submittedDate") val submittedDate: String? = null,
    @JsonProperty("sponsors") val sponsors: List<CongressSponsor> = emptyList(),
    @JsonProperty("latestAction") val latestAction: CongressAction? = null,
)

// Summaries DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSummariesResponse(
    @JsonProperty("summaries") val summaries: List<CongressDetailedSummary> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressDetailedSummary(
    @JsonProperty("actionDate") val actionDate: String? = null,
    @JsonProperty("actionDesc") val actionDesc: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("updateDate") val updateDate: String? = null,
    @JsonProperty("versionCode") val versionCode: String? = null,
    @JsonProperty("bill") val bill: CongressBillReference? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressBillReference(
    @JsonProperty("congress") val congress: Int? = null,
    @JsonProperty("number") val number: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("url") val url: String? = null,
)

// Members DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressMembersResponse(
    @JsonProperty("members") val members: List<CongressMember> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressMember(
    @JsonProperty("bioguideId") val bioguideId: String? = null,
    @JsonProperty("firstName") val firstName: String? = null,
    @JsonProperty("lastName") val lastName: String? = null,
    @JsonProperty("middleName") val middleName: String? = null,
    @JsonProperty("party") val party: String? = null,
    @JsonProperty("state") val state: String? = null,
    @JsonProperty("district") val district: String? = null,
    @JsonProperty("chamber") val chamber: String? = null,
    @JsonProperty("terms") val terms: List<CongressTerm> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTerm(
    @JsonProperty("chamber") val chamber: String? = null,
    @JsonProperty("startYear") val startYear: Int? = null,
    @JsonProperty("endYear") val endYear: Int? = null,
    @JsonProperty("memberType") val memberType: String? = null,
    @JsonProperty("stateCode") val stateCode: String? = null,
    @JsonProperty("stateName") val stateName: String? = null,
)

// Committee Reports DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressReportsResponse(
    @JsonProperty("reports") val reports: List<CongressReport> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressReport(
    @JsonProperty("congress") val congress: Int? = null,
    @JsonProperty("number") val number: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("publishedDate") val publishedDate: String? = null,
    @JsonProperty("committees") val committees: List<CongressCommittee> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCommittee(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("chamber") val chamber: String? = null,
    @JsonProperty("systemCode") val systemCode: String? = null,
    @JsonProperty("url") val url: String? = null,
)

// Nominations DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressNominationsResponse(
    @JsonProperty("nominations") val nominations: List<CongressNomination> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressNomination(
    @JsonProperty("congress") val congress: Int? = null,
    @JsonProperty("number") val number: String? = null,
    @JsonProperty("receivedDate") val receivedDate: String? = null,
    @JsonProperty("organization") val organization: String? = null,
    @JsonProperty("nominees") val nominees: List<CongressNominee> = emptyList(),
    @JsonProperty("latestAction") val latestAction: CongressAction? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressNominee(
    @JsonProperty("firstName") val firstName: String? = null,
    @JsonProperty("lastName") val lastName: String? = null,
    @JsonProperty("middleName") val middleName: String? = null,
    @JsonProperty("suffix") val suffix: String? = null,
    @JsonProperty("position") val position: String? = null,
)

// Treaties DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTreatiesResponse(
    @JsonProperty("treaties") val treaties: List<CongressTreaty> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTreaty(
    @JsonProperty("congress") val congress: Int? = null,
    @JsonProperty("number") val number: String? = null,
    @JsonProperty("suffix") val suffix: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("receivedDate") val receivedDate: String? = null,
    @JsonProperty("transmittedDate") val transmittedDate: String? = null,
    @JsonProperty("latestAction") val latestAction: CongressAction? = null,
)

// Text Versions DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTextVersionsResponse(
    @JsonProperty("textVersions") val textVersions: List<CongressTextVersion> = emptyList(),
    @JsonProperty("pagination") val pagination: CongressPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTextVersion(
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("date") val date: String? = null,
    @JsonProperty("formats") val formats: List<CongressTextFormat> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTextFormat(
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("url") val url: String? = null,
)
