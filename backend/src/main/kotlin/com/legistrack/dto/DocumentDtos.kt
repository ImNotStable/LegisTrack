package com.legistrack.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class DocumentSummaryDto(
    val id: Long,
    val billId: String,
    val title: String,
    val introductionDate: LocalDate?,
    val status: String?,
    val industryTags: List<String> = emptyList(),
    val partyBreakdown: PartyBreakdownDto,
    val hasValidAnalysis: Boolean,
)

data class DocumentDetailDto(
    val id: Long,
    val billId: String,
    val title: String,
    val officialSummary: String?,
    val introductionDate: LocalDate?,
    val congressSession: Int?,
    val billType: String?,
    val fullTextUrl: String?,
    val status: String?,
    val sponsors: List<SponsorDto> = emptyList(),
    val actions: List<DocumentActionDto> = emptyList(),
    val analysis: AiAnalysisDto?,
    val partyBreakdown: PartyBreakdownDto,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class SponsorDto(
    val id: Long,
    val bioguideId: String,
    val firstName: String?,
    val lastName: String?,
    val party: String?,
    val state: String?,
    val district: String?,
    val isPrimarySponsor: Boolean,
    val sponsorDate: LocalDate?,
)

data class DocumentActionDto(
    val id: Long,
    val actionDate: LocalDate,
    val actionType: String?,
    val actionText: String,
    val chamber: String?,
    val actionCode: String?,
)

data class AiAnalysisDto(
    val id: Long,
    val generalEffectText: String?,
    val economicEffectText: String?,
    val industryTags: List<String> = emptyList(),
    val isValid: Boolean,
    val analysisDate: LocalDateTime,
    val modelUsed: String?,
)

data class PartyBreakdownDto(
    val democratic: Int = 0,
    val republican: Int = 0,
    val independent: Int = 0,
    val other: Int = 0,
    val total: Int = 0,
    val democraticPercentage: Double = 0.0,
    val republicanPercentage: Double = 0.0,
)

data class InvalidateAnalysisRequest(
    val analysisId: Long,
)
