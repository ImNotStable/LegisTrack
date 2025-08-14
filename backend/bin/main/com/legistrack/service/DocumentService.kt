package com.legistrack.service

import com.legistrack.dto.AiAnalysisDto
import com.legistrack.dto.DocumentActionDto
import com.legistrack.dto.DocumentDetailDto
import com.legistrack.dto.DocumentSummaryDto
import com.legistrack.dto.PartyBreakdownDto
import com.legistrack.dto.SponsorDto
import com.legistrack.entity.AiAnalysis
import com.legistrack.entity.Document
import com.legistrack.entity.DocumentAction
import com.legistrack.entity.DocumentSponsor
import com.legistrack.repository.AiAnalysisRepository
import com.legistrack.repository.DocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing legislative documents and their analyses.
 *
 * Provides business logic for document retrieval, analysis validation,
 * and data transformation for API responses.
 */
@Service
@Transactional
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
) {
    /**
     * Retrieves all documents with pagination.
     *
     * Uses safe approach to avoid collection fetch warnings.
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated document summaries
     */
    @Transactional(readOnly = true)
    fun getAllDocuments(pageable: Pageable): Page<DocumentSummaryDto> {
        // Get paginated documents (no collection fetch warnings since no relationships in query)
        val documentsPage = documentRepository.findAllWithValidAnalyses(pageable)

        // Convert to DTOs using separate queries for each document
        return documentsPage.map { document ->
            val documentId = requireNotNull(document.id) { "Document ID cannot be null" }

            // Fetch related data separately for summary
            val sponsors = documentRepository.findSponsorsByDocumentId(documentId)
            val analyses = documentRepository.findAnalysesByDocumentId(documentId)

            document.toSummaryDto(sponsors, analyses)
        }
    }

    /**
     * Retrieves detailed information for a specific document.
     * Uses hardcoded response to test endpoint is working.
     *
     * @param id Document ID
     * @return Document details or null if not found
     */
    @Transactional(readOnly = true)
    fun getDocumentById(id: Long): DocumentDetailDto? {
        // Return a hardcoded response to test the endpoint
        return DocumentDetailDto(
            id = id,
            billId = "TEST-$id",
            title = "Test Document",
            officialSummary = "This is a test document",
            introductionDate = null,
            congressSession = 119,
            billType = "HR",
            fullTextUrl = null,
            status = "Test Status",
            sponsors = emptyList(),
            actions = emptyList(),
            analysis = null,
            partyBreakdown =
                PartyBreakdownDto(
                    democratic = 0,
                    republican = 0,
                    independent = 0,
                    other = 0,
                    total = 0,
                    democraticPercentage = 0.0,
                    republicanPercentage = 0.0,
                ),
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now(),
        )
    }

    /**
     * Invalidates an AI analysis by marking it as invalid.
     *
     * @param analysisId ID of the analysis to invalidate
     * @return true if analysis was successfully invalidated, false otherwise
     */
    fun invalidateAnalysis(analysisId: Long): Boolean {
        val updatedRows = aiAnalysisRepository.invalidateAnalysis(analysisId)
        return updatedRows > 0
    }

    /**
     * Converts a Document entity to a summary DTO with provided collections.
     *
     * For paginated results, we use separate queries to avoid collection issues.
     */
    private fun Document.toSummaryDto(
        sponsorList: List<DocumentSponsor>,
        analysisList: List<AiAnalysis>,
    ): DocumentSummaryDto {
        val validAnalysis = analysisList.firstOrNull { it.isValid }
        val partyBreakdown = sponsorList.calculatePartyBreakdown()

        return DocumentSummaryDto(
            id = requireNotNull(id) { "Document ID cannot be null" },
            billId = billId,
            title = title,
            introductionDate = introductionDate,
            status = status,
            industryTags = validAnalysis?.industryTags?.toList() ?: emptyList(),
            partyBreakdown = partyBreakdown,
            hasValidAnalysis = validAnalysis != null,
        )
    }

    /**
     * Converts a Document entity to a detailed DTO with provided collections.
     */
    private fun Document.toDetailDto(
        sponsorList: List<DocumentSponsor>,
        actionList: List<DocumentAction>,
        analysisList: List<AiAnalysis>,
    ): DocumentDetailDto {
        val validAnalysis = analysisList.firstOrNull { it.isValid }
        val partyBreakdown = sponsorList.calculatePartyBreakdown()

        return DocumentDetailDto(
            id = requireNotNull(id) { "Document ID cannot be null" },
            billId = billId,
            title = title,
            officialSummary = officialSummary,
            introductionDate = introductionDate,
            congressSession = congressSession,
            billType = billType,
            fullTextUrl = fullTextUrl,
            status = status,
            sponsors = sponsorList.map { it.toDto() },
            actions = actionList.sortedByDescending { it.actionDate }.map { it.toDto() },
            analysis = validAnalysis?.toDto(),
            partyBreakdown = partyBreakdown,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    /**
     * Converts a Document entity to a detailed DTO using entity collections.
     */
    private fun Document.toDetailDto(): DocumentDetailDto {
        val validAnalysis = analyses.firstOrNull { it.isValid }
        val partyBreakdown = sponsors.toList().calculatePartyBreakdown()

        return DocumentDetailDto(
            id = requireNotNull(id) { "Document ID cannot be null" },
            billId = billId,
            title = title,
            officialSummary = officialSummary,
            introductionDate = introductionDate,
            congressSession = congressSession,
            billType = billType,
            fullTextUrl = fullTextUrl,
            status = status,
            sponsors = sponsors.map { it.toDto() },
            actions = actions.sortedByDescending { it.actionDate }.map { it.toDto() },
            analysis = validAnalysis?.toDto(),
            partyBreakdown = partyBreakdown,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    /**
     * Converts a DocumentSponsor entity to a DTO.
     */
    private fun DocumentSponsor.toDto(): SponsorDto =
        SponsorDto(
            id = requireNotNull(sponsor.id) { "Sponsor ID cannot be null" },
            bioguideId = sponsor.bioguideId,
            firstName = sponsor.firstName,
            lastName = sponsor.lastName,
            party = sponsor.party,
            state = sponsor.state,
            district = sponsor.district,
            isPrimarySponsor = isPrimarySponsor,
            sponsorDate = sponsorDate,
        )

    /**
     * Converts a DocumentAction entity to a DTO.
     */
    private fun DocumentAction.toDto(): DocumentActionDto =
        DocumentActionDto(
            id = requireNotNull(id) { "Action ID cannot be null" },
            actionDate = actionDate,
            actionType = actionType,
            actionText = actionText,
            chamber = chamber,
            actionCode = actionCode,
        )

    /**
     * Converts an AiAnalysis entity to a DTO.
     */
    private fun AiAnalysis.toDto(): AiAnalysisDto =
        AiAnalysisDto(
            id = requireNotNull(id) { "Analysis ID cannot be null" },
            generalEffectText = generalEffectText,
            economicEffectText = economicEffectText,
            industryTags = industryTags.toList(),
            isValid = isValid,
            analysisDate = analysisDate,
            modelUsed = modelUsed,
        )

    /**
     * Calculates party breakdown from a list of document sponsors.
     */
    private fun List<DocumentSponsor>.calculatePartyBreakdown(): PartyBreakdownDto {
        val partyGroups =
            groupBy { sponsor ->
                when (sponsor.sponsor.party?.uppercase()) {
                    "D", "DEM", "DEMOCRATIC" -> "Democratic"
                    "R", "REP", "REPUBLICAN" -> "Republican"
                    "I", "IND", "INDEPENDENT" -> "Independent"
                    else -> "Other"
                }
            }

        val democratic = partyGroups["Democratic"]?.size ?: 0
        val republican = partyGroups["Republican"]?.size ?: 0
        val independent = partyGroups["Independent"]?.size ?: 0
        val other = partyGroups["Other"]?.size ?: 0
        val total = democratic + republican + independent + other

        val democraticPercentage = if (total > 0) (democratic.toDouble() / total) * 100 else 0.0
        val republicanPercentage = if (total > 0) (republican.toDouble() / total) * 100 else 0.0

        return PartyBreakdownDto(
            democratic = democratic,
            republican = republican,
            independent = independent,
            other = other,
            total = total,
            democraticPercentage = democraticPercentage,
            republicanPercentage = republicanPercentage,
        )
    }
}
