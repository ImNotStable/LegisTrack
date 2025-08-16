package com.legistrack.persistence.adapter

import com.legistrack.domain.dto.DocumentBasicDto
import com.legistrack.domain.dto.SponsorDto
import com.legistrack.domain.dto.DocumentActionDto
import com.legistrack.domain.dto.AiAnalysisDto
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.Document
import com.legistrack.domain.entity.DocumentAction
import com.legistrack.domain.entity.DocumentSponsor
import com.legistrack.domain.common.Page
import com.legistrack.domain.common.PageRequest
import com.legistrack.persistence.repository.DocumentRepository
import com.legistrack.persistence.repository.AiAnalysisRepository
import com.legistrack.persistence.repository.DocumentActionRepository
import com.legistrack.persistence.repository.DocumentSponsorRepository
import com.legistrack.persistence.mapper.EntityDomainMapper
import com.legistrack.domain.port.DocumentRepositoryPort
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort as SpringSort
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * JPA implementation of DocumentRepositoryPort.
 * 
 * Adapts the JPA repositories to the domain repository port interface,
 * handling the conversion between JPA entities and domain entities.
 */
@Service
class DocumentRepositoryAdapter(
    private val documentRepository: DocumentRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val documentActionRepository: DocumentActionRepository,
    private val documentSponsorRepository: DocumentSponsorRepository,
    private val mapper: EntityDomainMapper
) : DocumentRepositoryPort {

    override fun save(document: Document): Document {
        val jpaEntity = mapper.toJpaEntity(document)
        val savedEntity = documentRepository.save(jpaEntity)
        return mapper.toDomainEntity(savedEntity)
    }

    override fun saveAll(documents: List<Document>): List<Document> {
        val jpaEntities = documents.map { mapper.toJpaEntity(it) }
        val savedEntities = documentRepository.saveAll(jpaEntities)
        return savedEntities.map { mapper.toDomainEntity(it) }
    }

    override fun findById(id: Long): Document? {
        return documentRepository.findById(id).orElse(null)?.let { 
            mapper.toDomainEntity(it)
        }
    }

    override fun findByBillId(billId: String): Document? {
        return documentRepository.findByBillId(billId)?.let { 
            mapper.toDomainEntity(it)
        }
    }

    override fun existsByBillId(billId: String): Boolean {
        return documentRepository.existsByBillId(billId)
    }

    override fun findByIntroductionDateAfter(date: LocalDate): List<Document> {
        return documentRepository.findByIntroductionDateAfter(date)
            .map { mapper.toDomainEntity(it) }
    }

    override fun findAllWithValidAnalyses(pageRequest: PageRequest): Page<Document> {
        val springPageable = convertToSpringPageable(pageRequest)
        val springPage = documentRepository.findAllWithValidAnalyses(springPageable)
        return convertToDomainPage(springPage) { mapper.toDomainEntity(it) }
    }

    override fun searchDocuments(query: String, pageRequest: PageRequest): Page<Document> {
        val springPageable = convertToSpringPageable(pageRequest)
        val springPage = documentRepository.searchDocuments(query, springPageable)
        return convertToDomainPage(springPage) { mapper.toDomainEntity(it) }
    }

    override fun findByIndustryTag(tag: String, pageRequest: PageRequest): Page<Document> {
        val springPageable = convertToSpringPageable(pageRequest)
        val springPage = documentRepository.findByIndustryTag(tag, springPageable)
        return convertToDomainPage(springPage) { mapper.toDomainEntity(it) }
    }

    override fun findDocumentBasicById(id: Long): DocumentBasicDto? {
        return documentRepository.findDocumentBasicById(id)
    }

    override fun findByIdWithDetails(id: Long): Document? {
        return documentRepository.findByIdWithDetails(id)?.let { 
            mapper.toDomainEntity(it)
        }
    }

    override fun findSponsorsByDocumentId(documentId: Long): List<SponsorDto> {
        return documentRepository.findSponsorsByDocumentId(documentId)
            .map { jpaSponsor ->
                SponsorDto(
                    id = jpaSponsor.sponsor.id ?: 0L,
                    bioguideId = jpaSponsor.sponsor.bioguideId,
                    firstName = jpaSponsor.sponsor.firstName,
                    lastName = jpaSponsor.sponsor.lastName,
                    party = jpaSponsor.sponsor.party,
                    state = jpaSponsor.sponsor.state,
                    district = jpaSponsor.sponsor.district,
                    isPrimarySponsor = jpaSponsor.isPrimarySponsor,
                    sponsorDate = jpaSponsor.sponsorDate
                )
            }
    }

    override fun findActionsByDocumentId(documentId: Long): List<DocumentActionDto> {
        return documentRepository.findActionsByDocumentId(documentId)
            .map { jpaAction ->
                DocumentActionDto(
                    id = jpaAction.id ?: 0L,
                    actionDate = jpaAction.actionDate,
                    actionType = jpaAction.actionType,
                    actionText = jpaAction.actionText,
                    chamber = jpaAction.chamber,
                    actionCode = jpaAction.actionCode
                )
            }
    }

    override fun findAnalysesByDocumentId(documentId: Long): List<AiAnalysisDto> {
        return documentRepository.findAnalysesByDocumentId(documentId)
            .map { jpaAnalysis ->
                AiAnalysisDto(
                    id = jpaAnalysis.id ?: 0L,
                    generalEffectText = jpaAnalysis.generalEffectText,
                    economicEffectText = jpaAnalysis.economicEffectText,
                    industryTags = jpaAnalysis.industryTags.toList(),
                    isValid = jpaAnalysis.isValid,
                    analysisDate = jpaAnalysis.analysisDate,
                    modelUsed = jpaAnalysis.modelUsed
                )
            }
    }

    override fun countDocumentsNeedingAnalysis(): Long {
        return documentRepository.countDocumentsNeedingAnalysis()
    }

    override fun deleteById(id: Long) {
        documentRepository.deleteById(id)
    }

    override fun deleteAll() {
        documentRepository.deleteAll()
    }

    override fun count(): Long {
        return documentRepository.count()
    }

    override fun existsAny(): Boolean {
        return documentRepository.count() > 0
    }
    
    /**
     * Converts domain PageRequest to Spring Pageable.
     */
    private fun convertToSpringPageable(pageRequest: PageRequest): Pageable {
        val sort = pageRequest.sort?.let { domainSort ->
            val orders = domainSort.orders.map { order ->
                when (order.direction) {
                    com.legistrack.domain.common.Sort.Direction.ASC -> 
                        SpringSort.Order.asc(order.property)
                    com.legistrack.domain.common.Sort.Direction.DESC -> 
                        SpringSort.Order.desc(order.property)
                }
            }
            SpringSort.by(orders)
        } ?: SpringSort.unsorted()
        
        return SpringPageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, sort)
    }
    
    /**
     * Converts Spring Page to domain Page.
     */
    private fun <T, U> convertToDomainPage(
        springPage: org.springframework.data.domain.Page<T>, 
        transform: (T) -> U
    ): Page<U> {
        return Page(
            content = springPage.content.map(transform),
            totalElements = springPage.totalElements,
            totalPages = springPage.totalPages,
            pageNumber = springPage.number,
            pageSize = springPage.size,
            isFirst = springPage.isFirst,
            isLast = springPage.isLast,
            hasNext = springPage.hasNext(),
            hasPrevious = springPage.hasPrevious()
        )
    }
}