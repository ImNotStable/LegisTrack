package com.legistrack.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// Congress.gov API DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressBillsResponse(
    val bills: List<CongressBill> = emptyList(),
    val pagination: CongressPagination? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressBill(
    val congress: Int? = null,
    val number: String? = null,
    val type: String? = null,
    val title: String? = null,
    @JsonProperty("introducedDate")
    val introducedDate: String? = null,
    val sponsors: List<CongressSponsor> = emptyList(),
    val cosponsors: CongressCosponsors? = null,
    val actions: CongressActions? = null,
    val summaries: List<CongressSummary> = emptyList(),
    val textVersions: CongressTextVersions? = null,
    val latestAction: CongressAction? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSponsor(
    val bioguideId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val party: String? = null,
    val state: String? = null,
    val district: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCosponsors(
    val count: Int = 0,
    val countIncludingWithdrawnCosponsors: Int = 0,
    val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressActions(
    val count: Int = 0,
    val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressAction(
    val actionDate: String? = null,
    val text: String? = null,
    val type: String? = null,
    val actionCode: String? = null,
    val sourceSystem: CongressSourceSystem? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSourceSystem(
    val code: String? = null,
    val name: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressSummary(
    val actionDate: String? = null,
    val actionDesc: String? = null,
    val text: String? = null,
    val updateDate: String? = null,
    val versionCode: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressTextVersions(
    val count: Int = 0,
    val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressPagination(
    val count: Int = 0,
    val next: String? = null,
    val prev: String? = null
)

// Detailed sponsor response
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCosponsorsResponse(
    val cosponsors: List<CongressCosponsor> = emptyList(),
    val pagination: CongressPagination? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressCosponsor(
    val bioguideId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val party: String? = null,
    val state: String? = null,
    val district: String? = null,
    val sponsorshipDate: String? = null,
    val sponsorshipWithdrawnDate: String? = null
)

// Detailed actions response
@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressActionsResponse(
    val actions: List<CongressDetailedAction> = emptyList(),
    val pagination: CongressPagination? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CongressDetailedAction(
    val actionDate: String? = null,
    val text: String? = null,
    val type: String? = null,
    val actionCode: String? = null,
    val chamber: String? = null,
    val sourceSystem: CongressSourceSystem? = null
)
