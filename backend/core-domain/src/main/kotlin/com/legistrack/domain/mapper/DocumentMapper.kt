package com.legistrack.domain.mapper

import com.legistrack.domain.entity.Document
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.DocumentAction
import com.legistrack.domain.entity.DocumentSponsor
import com.legistrack.domain.entity.Sponsor
import com.legistrack.domain.dto.DocumentBasicDto
import com.legistrack.domain.dto.DocumentDetailDto
import com.legistrack.domain.dto.DocumentSummaryDto
import com.legistrack.domain.dto.SponsorDto
import com.legistrack.domain.dto.DocumentActionDto
import com.legistrack.domain.dto.AiAnalysisDto
import com.legistrack.domain.dto.PartyBreakdownDto

/**
 * Mapper utilities for converting between domain entities and DTOs.
 */
object DocumentMapper {

    /**
     * Converts Document entity to DocumentBasicDto.
     */
    fun toBasicDto(document: Document): DocumentBasicDto = DocumentBasicDto(
        id = document.id ?: 0L,
        billId = document.billId,
        title = document.title,
        officialSummary = document.officialSummary,
        introductionDate = document.introductionDate,
        congressSession = document.congressSession ?: 0,
        billType = document.billType ?: "",
        fullTextUrl = document.fullTextUrl,
        status = document.status ?: "",
        createdAt = document.createdAt,
        updatedAt = document.updatedAt
    )

    /**
     * Converts Document entity to DocumentSummaryDto.
     */
    fun toSummaryDto(
        document: Document,
        industryTags: List<String> = emptyList(),
        partyBreakdown: PartyBreakdownDto = PartyBreakdownDto(),
        hasValidAnalysis: Boolean = false
    ): DocumentSummaryDto = DocumentSummaryDto(
        id = document.id ?: 0L,
        billId = document.billId,
        title = document.title,
        introductionDate = document.introductionDate,
        status = document.status,
        industryTags = industryTags,
        partyBreakdown = partyBreakdown,
        hasValidAnalysis = hasValidAnalysis
    )

    /**
     * Converts Document entity to DocumentDetailDto.
     */
    fun toDetailDto(
        document: Document,
        sponsors: List<SponsorDto> = emptyList(),
        actions: List<DocumentActionDto> = emptyList(),
        analysis: AiAnalysisDto? = null,
        partyBreakdown: PartyBreakdownDto = PartyBreakdownDto()
    ): DocumentDetailDto = DocumentDetailDto(
        id = document.id ?: 0L,
        billId = document.billId,
        title = document.title,
        officialSummary = document.officialSummary,
        introductionDate = document.introductionDate,
        congressSession = document.congressSession,
        billType = document.billType,
        fullTextUrl = document.fullTextUrl,
        status = document.status,
        sponsors = sponsors,
        actions = actions,
        analysis = analysis,
        partyBreakdown = partyBreakdown,
        createdAt = document.createdAt,
        updatedAt = document.updatedAt
    )

    /**
     * Converts Sponsor and DocumentSponsor to SponsorDto.
     */
    fun toSponsorDto(sponsor: Sponsor, documentSponsor: DocumentSponsor): SponsorDto = SponsorDto(
        id = sponsor.id ?: 0L,
        bioguideId = sponsor.bioguideId,
        firstName = sponsor.firstName,
        lastName = sponsor.lastName,
        party = sponsor.party,
        state = sponsor.state,
        district = sponsor.district,
        isPrimarySponsor = documentSponsor.isPrimarySponsor,
        sponsorDate = documentSponsor.sponsorDate
    )

    /**
     * Converts DocumentAction to DocumentActionDto.
     */
    fun toActionDto(action: DocumentAction): DocumentActionDto = DocumentActionDto(
        id = action.id ?: 0L,
        actionDate = action.actionDate,
        actionType = action.actionType,
        actionText = action.actionText,
        chamber = action.chamber,
        actionCode = action.actionCode
    )

    /**
     * Converts AiAnalysis to AiAnalysisDto.
     */
    fun toAnalysisDto(analysis: AiAnalysis): AiAnalysisDto = AiAnalysisDto(
        id = analysis.id ?: 0L,
        generalEffectText = analysis.generalEffectText,
        economicEffectText = analysis.economicEffectText,
        industryTags = analysis.industryTags,
        isValid = analysis.isValid,
        analysisDate = analysis.analysisDate,
        modelUsed = analysis.modelUsed
    )

    /**
     * Calculates party breakdown from a list of sponsors.
     */
    fun calculatePartyBreakdown(sponsors: List<SponsorDto>): PartyBreakdownDto {
        val total = sponsors.size
        if (total == 0) return PartyBreakdownDto()

        val democratic = sponsors.count { it.party?.lowercase() == "democratic" || it.party?.lowercase() == "d" }
        val republican = sponsors.count { it.party?.lowercase() == "republican" || it.party?.lowercase() == "r" }
        val independent = sponsors.count { it.party?.lowercase() == "independent" || it.party?.lowercase() == "i" }
        val other = total - democratic - republican - independent

        return PartyBreakdownDto(
            democratic = democratic,
            republican = republican,
            independent = independent,
            other = other,
            total = total,
            democraticPercentage = if (total > 0) (democratic.toDouble() / total) * 100 else 0.0,
            republicanPercentage = if (total > 0) (republican.toDouble() / total) * 100 else 0.0
        )
    }

    /**
     * Converts DocumentBasicDto back to Document entity.
     */
    fun fromBasicDto(dto: DocumentBasicDto): Document = Document(
        id = dto.id,
        billId = dto.billId,
        title = dto.title,
        officialSummary = dto.officialSummary,
        introductionDate = dto.introductionDate,
        congressSession = dto.congressSession,
        billType = dto.billType,
        fullTextUrl = dto.fullTextUrl,
        status = dto.status,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt
    )
}