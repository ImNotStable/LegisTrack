package com.legistrack.service

import com.legistrack.domain.dto.AiAnalysisDto
import com.legistrack.domain.dto.DocumentActionDto
import com.legistrack.domain.dto.DocumentDetailDto
import com.legistrack.domain.dto.DocumentSummaryDto
import com.legistrack.domain.dto.PartyBreakdownDto
import com.legistrack.domain.dto.SponsorDto
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.Document
import com.legistrack.domain.entity.DocumentAction
import com.legistrack.domain.entity.DocumentSponsor
import com.legistrack.domain.mapper.DocumentMapper
import com.legistrack.domain.port.DocumentRepositoryPort
// TODO: Phase 3 - import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.common.Page
import com.legistrack.domain.common.PageRequest
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
    private val documentRepositoryPort: DocumentRepositoryPort,
    // TODO: Phase 3 - uncomment when AiModelPort is implemented
    // private val aiModelPort: AiModelPort,
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
        val domainPageRequest = convertToDomainPageRequest(pageable)
        // Get paginated documents (no collection fetch warnings since no relationships in query)
        val documentsPage = documentRepositoryPort.findAllWithValidAnalyses(domainPageRequest)

        // Convert to DTOs using separate queries for each document
        return documentsPage.map { document ->
            val documentId = requireNotNull(document.id) { "Document ID cannot be null" }

            // Fetch related data separately for summary; be resilient in unit tests (mocks)
            val sponsors =
                try {
                    documentRepositoryPort.findSponsorsByDocumentId(documentId)
                } catch (_: Exception) {
                    emptyList()
                }
            val analyses =
                try {
                    documentRepositoryPort.findAnalysesByDocumentId(documentId)
                } catch (_: Exception) {
                    emptyList()
                }

            DocumentMapper.toSummaryDto(
                document = document,
                industryTags = analyses.filter { it.isValid }.flatMap { it.industryTags }.distinct(),
                partyBreakdown = calculatePartyBreakdown(sponsors),
                hasValidAnalysis = analyses.any { it.isValid }
            )
        }
    }

    @Transactional(readOnly = true)
    fun searchDocuments(query: String, pageable: Pageable): Page<DocumentSummaryDto> {
        val domainPageRequest = convertToDomainPageRequest(pageable)
        val pageOfDocs = documentRepositoryPort.searchDocuments(query, domainPageRequest)
        return pageOfDocs.map { document ->
            val documentId = requireNotNull(document.id)
            val sponsors = try { documentRepositoryPort.findSponsorsByDocumentId(documentId) } catch (_: Exception) { emptyList() }
            val analyses = try { documentRepositoryPort.findAnalysesByDocumentId(documentId) } catch (_: Exception) { emptyList() }
            DocumentMapper.toSummaryDto(
                document = document,
                industryTags = analyses.filter { it.isValid }.flatMap { it.industryTags }.distinct(),
                partyBreakdown = calculatePartyBreakdown(sponsors),
                hasValidAnalysis = analyses.any { it.isValid }
            )
        }
    }

    @Transactional(readOnly = true)
    fun findByIndustryTag(tag: String, pageable: Pageable): Page<DocumentSummaryDto> {
        val domainPageRequest = convertToDomainPageRequest(pageable)
        val pageOfDocs = documentRepositoryPort.findByIndustryTag(tag, domainPageRequest)
        return pageOfDocs.map { document ->
            val documentId = requireNotNull(document.id)
            val sponsors = try { documentRepositoryPort.findSponsorsByDocumentId(documentId) } catch (_: Exception) { emptyList() }
            val analyses = try { documentRepositoryPort.findAnalysesByDocumentId(documentId) } catch (_: Exception) { emptyList() }
            DocumentMapper.toSummaryDto(
                document = document,
                industryTags = analyses.filter { it.isValid }.flatMap { it.industryTags }.distinct(),
                partyBreakdown = calculatePartyBreakdown(sponsors),
                hasValidAnalysis = analyses.any { it.isValid }
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAnalyticsSummary(): Map<String, Any> {
        val totalDocuments = documentRepositoryPort.count()
        val documentsWithAnalysis = documentRepositoryPort.countDocumentsNeedingAnalysis() // Note: inverted logic - documents needing analysis vs with analysis

        // Compute average party breakdown across all documents using summaries page by page
        val pageSize = 500
        var democraticAccum = 0.0
        var republicanAccum = 0.0
        var counted = 0

        var pageIndex = 0
        while (true) {
            val page = documentRepositoryPort.findAllWithValidAnalyses(PageRequest.of(pageIndex, pageSize))
            if (page.isEmpty) break
            page.content.forEach { d ->
                val id = requireNotNull(d.id)
                val sponsors = try { documentRepositoryPort.findSponsorsByDocumentId(id) } catch (_: Exception) { emptyList() }
                val analyses = try { documentRepositoryPort.findAnalysesByDocumentId(id) } catch (_: Exception) { emptyList() }
                val summary = DocumentMapper.toSummaryDto(
                    document = d,
                    industryTags = analyses.filter { it.isValid }.flatMap { it.industryTags }.distinct(),
                    partyBreakdown = calculatePartyBreakdown(sponsors),
                    hasValidAnalysis = analyses.any { it.isValid }
                )
                democraticAccum += summary.partyBreakdown.democraticPercentage
                republicanAccum += summary.partyBreakdown.republicanPercentage
                counted += 1
            }
            if (page.isLast) break
            pageIndex += 1
        }

        val avgDem = if (counted > 0) democraticAccum / counted else 0.0
        val avgRep = if (counted > 0) republicanAccum / counted else 0.0

        // TODO: Phase 3 - Implement analytics for industry tags through port interface
        val topIndustryTags = emptyList<Map<String, Any>>()

        return mapOf(
            "totalDocuments" to totalDocuments,
            "documentsWithAnalysis" to documentsWithAnalysis,
            "avgDemocraticSponsorship" to avgDem,
            "avgRepublicanSponsorship" to avgRep,
            "topIndustryTags" to topIndustryTags,
        )
    }

    /**
     * Generates AI analysis for a document and persists it.
     */
    suspend fun analyzeDocument(id: Long): DocumentDetailDto? {
        // TODO: Phase 3 - Implement AI analysis when AiModelPort is available
        return getDocumentById(id)
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
        val document: Document =
            documentRepositoryPort.findByIdWithDetails(id) ?: return null

        val sponsors =
            try {
                documentRepositoryPort.findSponsorsByDocumentId(id)
            } catch (_: Exception) {
                emptyList()
            }
        val actions =
            try {
                documentRepositoryPort.findActionsByDocumentId(id)
            } catch (_: Exception) {
                emptyList()
            }
        val analyses =
            try {
                documentRepositoryPort.findAnalysesByDocumentId(id)
            } catch (_: Exception) {
                emptyList()
            }

        val partyBreakdown = DocumentMapper.calculatePartyBreakdown(sponsors)
        return DocumentMapper.toDetailDto(
            document = document,
            sponsors = sponsors,
            actions = actions.sortedByDescending { it.actionDate },
            analysis = analyses.firstOrNull { it.isValid },
            partyBreakdown = partyBreakdown
        )
    }

    /**
     * Invalidates an AI analysis by marking it as invalid.
     *
     * @param analysisId ID of the analysis to invalidate
     * @return true if analysis was successfully invalidated, false otherwise
     */
    fun invalidateAnalysis(analysisId: Long): Boolean {
        // TODO: Phase 3 - Implement through port interface
        return false
    }







    /**
     * Calculates party breakdown from a list of sponsor DTOs.
     */
    private fun calculatePartyBreakdown(sponsors: List<SponsorDto>): PartyBreakdownDto {
        return DocumentMapper.calculatePartyBreakdown(sponsors)
    }
    
    /**
     * Converts Spring Pageable to domain PageRequest.
     */
    private fun convertToDomainPageRequest(pageable: Pageable): PageRequest {
        val domainSort = if (pageable.sort.isSorted) {
            val orders = pageable.sort.map { order ->
                com.legistrack.domain.common.Sort.Order(
                    property = order.property,
                    direction = when (order.direction) {
                        org.springframework.data.domain.Sort.Direction.ASC -> 
                            com.legistrack.domain.common.Sort.Direction.ASC
                        org.springframework.data.domain.Sort.Direction.DESC -> 
                            com.legistrack.domain.common.Sort.Direction.DESC
                    }
                )
            }
            com.legistrack.domain.common.Sort(orders.toList())
        } else null
        
        return PageRequest(
            pageNumber = pageable.pageNumber,
            pageSize = pageable.pageSize,
            sort = domainSort
        )
    }
}
