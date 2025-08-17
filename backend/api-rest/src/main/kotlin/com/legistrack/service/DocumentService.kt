package com.legistrack.service

import com.legistrack.aianalysis.AiAnalysisService
import com.legistrack.domain.common.Page
import com.legistrack.domain.common.PageRequest
import com.legistrack.domain.dto.DocumentDetailDto
import com.legistrack.domain.dto.DocumentSummaryDto
import com.legistrack.domain.dto.PartyBreakdownDto
import com.legistrack.domain.dto.SponsorDto
import com.legistrack.domain.entity.Document
import com.legistrack.domain.mapper.DocumentMapper
import com.legistrack.domain.port.DocumentRepositoryPort
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class DocumentService(
    private val documentRepositoryPort: DocumentRepositoryPort,
    private val aiAnalysisService: AiAnalysisService,
) {
    @Transactional(readOnly = true)
    fun getAllDocuments(pageable: Pageable): Page<DocumentSummaryDto> {
        val domainPageRequest = convertToDomainPageRequest(pageable)
        val documentsPage = documentRepositoryPort.findAllWithValidAnalyses(domainPageRequest)
        return documentsPage.map { document -> mapToSummary(document) }
    }

    @Transactional(readOnly = true)
    fun searchDocuments(query: String, pageable: Pageable): Page<DocumentSummaryDto> {
        val domainPageRequest = convertToDomainPageRequest(pageable)
        return documentRepositoryPort.searchDocuments(query, domainPageRequest).map { mapToSummary(it) }
    }

    @Transactional(readOnly = true)
    fun findByIndustryTag(tag: String, pageable: Pageable): Page<DocumentSummaryDto> {
        val domainPageRequest = convertToDomainPageRequest(pageable)
        return documentRepositoryPort.findByIndustryTag(tag, domainPageRequest).map { mapToSummary(it) }
    }

    @Transactional(readOnly = true)
    fun getAnalyticsSummary(): Map<String, Any> {
        val totalDocuments = documentRepositoryPort.count()
        val documentsWithAnalysis = documentRepositoryPort.countDocumentsNeedingAnalysis()
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
        val sponsors = safeList { documentRepositoryPort.findSponsorsByDocumentId(id) }
        val analyses = safeList { documentRepositoryPort.findAnalysesByDocumentId(id) }
                val summary = DocumentMapper.toSummaryDto(
                    document = d,
                    industryTags = analyses.filter { it.isValid }.flatMap { it.industryTags }.distinct(),
                    partyBreakdown = calculatePartyBreakdown(sponsors),
                    hasValidAnalysis = analyses.any { it.isValid },
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
        return mapOf(
            "totalDocuments" to totalDocuments,
            "documentsWithAnalysis" to documentsWithAnalysis,
            "avgDemocraticSponsorship" to avgDem,
            "avgRepublicanSponsorship" to avgRep,
            "topIndustryTags" to emptyList<Map<String, Any>>()
        )
    }

    suspend fun analyzeDocument(id: Long): DocumentDetailDto? {
        val document = documentRepositoryPort.findById(id) ?: return null
        try { aiAnalysisService.generateAndPersist(document) } catch (_: Exception) {}
        return getDocumentById(id)
    }

    @Transactional(readOnly = true)
    fun getDocumentById(id: Long): DocumentDetailDto? {
        val document: Document = documentRepositoryPort.findByIdWithDetails(id) ?: return null
    val sponsors = safeList { documentRepositoryPort.findSponsorsByDocumentId(id) }
    val actions = safeList { documentRepositoryPort.findActionsByDocumentId(id) }.sortedByDescending { it.actionDate }
    val analyses = safeList { documentRepositoryPort.findAnalysesByDocumentId(id) }
        val partyBreakdown = DocumentMapper.calculatePartyBreakdown(sponsors)
        return DocumentMapper.toDetailDto(
            document = document,
            sponsors = sponsors,
            actions = actions,
            analysis = analyses.firstOrNull { it.isValid },
            partyBreakdown = partyBreakdown,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun invalidateAnalysis(_analysisId: Long): Boolean { return false }

    private fun mapToSummary(document: Document): DocumentSummaryDto {
        val documentId = requireNotNull(document.id) { "Document ID cannot be null" }
    val sponsors = safeList { documentRepositoryPort.findSponsorsByDocumentId(documentId) }
    val analyses = safeList { documentRepositoryPort.findAnalysesByDocumentId(documentId) }
        return DocumentMapper.toSummaryDto(
            document = document,
            industryTags = analyses.filter { it.isValid }.flatMap { it.industryTags }.distinct(),
            partyBreakdown = calculatePartyBreakdown(sponsors),
            hasValidAnalysis = analyses.any { it.isValid },
        )
    }

    private fun calculatePartyBreakdown(sponsors: List<SponsorDto>): PartyBreakdownDto = DocumentMapper.calculatePartyBreakdown(sponsors)

    private fun convertToDomainPageRequest(pageable: Pageable): PageRequest {
        // Input validation & clamping [RESILIENCE]: prevent unbounded page sizes exhausting DB resources.
        val maxPageSize = 100
        val sanitizedPageSize = when {
            pageable.pageSize <= 0 -> 20
            pageable.pageSize > maxPageSize -> maxPageSize
            else -> pageable.pageSize
        }
        val sanitizedPageNumber = if (pageable.pageNumber < 0) 0 else pageable.pageNumber
        val domainSort = if (pageable.sort.isSorted) {
            val orders = pageable.sort.map { order ->
                com.legistrack.domain.common.Sort.Order(
                    property = order.property,
                    direction = when (order.direction) {
                        org.springframework.data.domain.Sort.Direction.ASC -> com.legistrack.domain.common.Sort.Direction.ASC
                        org.springframework.data.domain.Sort.Direction.DESC -> com.legistrack.domain.common.Sort.Direction.DESC
                        else -> com.legistrack.domain.common.Sort.Direction.ASC
                    }
                )
            }
            com.legistrack.domain.common.Sort(orders.toList())
        } else null
    return PageRequest(pageNumber = sanitizedPageNumber, pageSize = sanitizedPageSize, sort = domainSort)
    }

    /**
     * Executes a repository call that returns a List, swallowing exceptions per resilience rules (returns empty list on failure).
     * This avoids unchecked casts that were previously generated by a generic fallback helper.
     */
    private inline fun <reified T> safeList(block: () -> List<T>): List<T> = try { block() } catch (_: Exception) { emptyList() }
}
