package com.legistrack.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// GovInfo API DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoCollectionsResponse(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("nextPage") val nextPage: String? = null,
    @JsonProperty("previousPage") val previousPage: String? = null,
    @JsonProperty("collections") val collections: List<GovInfoCollection> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoCollection(
    @JsonProperty("collectionCode") val collectionCode: String? = null,
    @JsonProperty("collectionName") val collectionName: String? = null,
    @JsonProperty("packageCount") val packageCount: Int = 0,
    @JsonProperty("granuleCount") val granuleCount: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoPackagesResponse(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("nextPage") val nextPage: String? = null,
    @JsonProperty("previousPage") val previousPage: String? = null,
    @JsonProperty("packages") val packages: List<GovInfoPackage> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoPackage(
    @JsonProperty("packageId") val packageId: String? = null,
    @JsonProperty("lastModified") val lastModified: String? = null,
    @JsonProperty("packageLink") val packageLink: String? = null,
    @JsonProperty("docClass") val docClass: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("congress") val congress: String? = null,
    @JsonProperty("dateIssued") val dateIssued: String? = null,
    @JsonProperty("collectionCode") val collectionCode: String? = null,
    @JsonProperty("collectionName") val collectionName: String? = null,
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("branch") val branch: String? = null,
    @JsonProperty("suDocClassNumber") val suDocClassNumber: String? = null,
    @JsonProperty("publishDate") val publishDate: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoPackageDetailsResponse(
    @JsonProperty("packageId") val packageId: String? = null,
    @JsonProperty("lastModified") val lastModified: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("collectionCode") val collectionCode: String? = null,
    @JsonProperty("collectionName") val collectionName: String? = null,
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("dateIssued") val dateIssued: String? = null,
    @JsonProperty("docClass") val docClass: String? = null,
    @JsonProperty("congress") val congress: String? = null,
    @JsonProperty("sessionType") val sessionType: String? = null,
    @JsonProperty("chamber") val chamber: String? = null,
    @JsonProperty("originChamber") val originChamber: String? = null,
    @JsonProperty("billType") val billType: String? = null,
    @JsonProperty("billNumber") val billNumber: String? = null,
    @JsonProperty("billVersion") val billVersion: String? = null,
    @JsonProperty("pages") val pages: String? = null,
    @JsonProperty("government") val government: String? = null,
    @JsonProperty("governmentBranch") val governmentBranch: String? = null,
    @JsonProperty("isPrivate") val isPrivate: Boolean = false,
    @JsonProperty("isAppropriation") val isAppropriation: Boolean = false,
    @JsonProperty("suDocClassNumber") val suDocClassNumber: String? = null,
    @JsonProperty("granules") val granules: GovInfoGranules? = null,
    @JsonProperty("download") val download: GovInfoDownload? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoGranules(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("granuleLink") val granuleLink: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoDownload(
    @JsonProperty("txtLink") val txtLink: String? = null,
    @JsonProperty("xmlLink") val xmlLink: String? = null,
    @JsonProperty("pdfLink") val pdfLink: String? = null,
    @JsonProperty("zipLink") val zipLink: String? = null,
    @JsonProperty("premiumLink") val premiumLink: String? = null,
    @JsonProperty("modsLink") val modsLink: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoGranulesResponse(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("nextPage") val nextPage: String? = null,
    @JsonProperty("previousPage") val previousPage: String? = null,
    @JsonProperty("granules") val granules: List<GovInfoGranule> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoGranule(
    @JsonProperty("granuleId") val granuleId: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("granuleLink") val granuleLink: String? = null,
    @JsonProperty("granuleClass") val granuleClass: String? = null,
    @JsonProperty("dateIssued") val dateIssued: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoGranuleDetailsResponse(
    @JsonProperty("granuleId") val granuleId: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("granuleClass") val granuleClass: String? = null,
    @JsonProperty("subGranuleClass") val subGranuleClass: String? = null,
    @JsonProperty("dateIssued") val dateIssued: String? = null,
    @JsonProperty("packageId") val packageId: String? = null,
    @JsonProperty("collectionCode") val collectionCode: String? = null,
    @JsonProperty("collectionName") val collectionName: String? = null,
    @JsonProperty("download") val download: GovInfoDownload? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoPublishedResponse(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("nextPage") val nextPage: String? = null,
    @JsonProperty("previousPage") val previousPage: String? = null,
    @JsonProperty("packages") val packages: List<GovInfoPackage> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoRelatedResponse(
    @JsonProperty("relationships") val relationships: List<GovInfoRelationship> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoRelationship(
    @JsonProperty("relationshipType") val relationshipType: String? = null,
    @JsonProperty("identifiers") val identifiers: List<GovInfoIdentifier> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoIdentifier(
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("identifier") val identifier: String? = null,
    @JsonProperty("collectionCode") val collectionCode: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("packageId") val packageId: String? = null,
    @JsonProperty("granuleId") val granuleId: String? = null,
)

// Search DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSearchRequest(
    @JsonProperty("query") val query: String,
    @JsonProperty("pageSize") val pageSize: Int = 20,
    @JsonProperty("offsetMark") val offsetMark: String = "*",
    @JsonProperty("sorts") val sorts: List<GovInfoSort> = listOf(GovInfoSort("relevancy", "DESC")),
    @JsonProperty("historical") val historical: Boolean = true,
    @JsonProperty("resultLevel") val resultLevel: String = "default",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSort(
    @JsonProperty("field") val field: String,
    @JsonProperty("sortOrder") val sortOrder: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSearchResponse(
    @JsonProperty("count") val count: Int = 0,
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("offsetMark") val offsetMark: String? = null,
    @JsonProperty("nextPage") val nextPage: String? = null,
    @JsonProperty("previousPage") val previousPage: String? = null,
    @JsonProperty("packages") val packages: List<GovInfoSearchResult> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSearchResult(
    @JsonProperty("packageId") val packageId: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("collectionCode") val collectionCode: String? = null,
    @JsonProperty("collectionName") val collectionName: String? = null,
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("dateIssued") val dateIssued: String? = null,
    @JsonProperty("lastModified") val lastModified: String? = null,
    @JsonProperty("packageLink") val packageLink: String? = null,
    @JsonProperty("docClass") val docClass: String? = null,
    @JsonProperty("congress") val congress: String? = null,
    @JsonProperty("billType") val billType: String? = null,
    @JsonProperty("billNumber") val billNumber: String? = null,
    @JsonProperty("relevancy") val relevancy: String? = null,
)

// Bill Status DTOs (subset for bills collection)
@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoBillStatus(
    @JsonProperty("bill") val bill: GovInfoBillDetails? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoBillDetails(
    @JsonProperty("congress") val congress: String? = null,
    @JsonProperty("originChamber") val originChamber: String? = null,
    @JsonProperty("billType") val billType: String? = null,
    @JsonProperty("billNumber") val billNumber: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("introducedDate") val introducedDate: String? = null,
    @JsonProperty("sponsors") val sponsors: List<GovInfoSponsor> = emptyList(),
    @JsonProperty("cosponsors") val cosponsors: List<GovInfoSponsor> = emptyList(),
    @JsonProperty("actions") val actions: List<GovInfoAction> = emptyList(),
    @JsonProperty("summaries") val summaries: List<GovInfoSummary> = emptyList(),
    @JsonProperty("subjects") val subjects: List<GovInfoSubject> = emptyList(),
    @JsonProperty("committees") val committees: List<GovInfoCommittee> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSponsor(
    @JsonProperty("bioguideId") val bioguideId: String? = null,
    @JsonProperty("fullName") val fullName: String? = null,
    @JsonProperty("firstName") val firstName: String? = null,
    @JsonProperty("lastName") val lastName: String? = null,
    @JsonProperty("party") val party: String? = null,
    @JsonProperty("state") val state: String? = null,
    @JsonProperty("district") val district: String? = null,
    @JsonProperty("isByRequest") val isByRequest: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoAction(
    @JsonProperty("actionDate") val actionDate: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("actionCode") val actionCode: String? = null,
    @JsonProperty("sourceSystem") val sourceSystem: GovInfoSourceSystem? = null,
    @JsonProperty("committee") val committee: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSourceSystem(
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("name") val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSummary(
    @JsonProperty("actionDate") val actionDate: String? = null,
    @JsonProperty("actionDesc") val actionDesc: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("updateDate") val updateDate: String? = null,
    @JsonProperty("versionCode") val versionCode: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoSubject(
    @JsonProperty("name") val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoCommittee(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("chamber") val chamber: String? = null,
    @JsonProperty("systemCode") val systemCode: String? = null,
    @JsonProperty("activities") val activities: List<GovInfoCommitteeActivity> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GovInfoCommitteeActivity(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("date") val date: String? = null,
)
