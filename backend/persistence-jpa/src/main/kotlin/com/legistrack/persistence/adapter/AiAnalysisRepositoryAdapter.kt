package com.legistrack.persistence.adapter

import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.port.AiAnalysisRepositoryPort
import com.legistrack.persistence.mapper.EntityDomainMapper
import com.legistrack.persistence.repository.AiAnalysisRepository
import org.springframework.stereotype.Component

/**
 * JPA implementation of AiAnalysisRepositoryPort.
 *
 * Adapts JPA-based AiAnalysisRepository to the domain port interface,
 * handling entity-domain mapping and Spring Data JPA operations.
 */
@Component
class AiAnalysisRepositoryAdapter(
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val entityDomainMapper: EntityDomainMapper,
) : AiAnalysisRepositoryPort {

    override fun save(analysis: AiAnalysis): AiAnalysis {
        val entity = entityDomainMapper.toAiAnalysisEntity(analysis)
        val savedEntity = aiAnalysisRepository.save(entity)
        return entityDomainMapper.toAiAnalysisDomain(savedEntity)
    }

    override fun saveAll(analyses: List<AiAnalysis>): List<AiAnalysis> {
        val entities = analyses.map(entityDomainMapper::toAiAnalysisEntity)
        val savedEntities = aiAnalysisRepository.saveAll(entities)
        return savedEntities.map(entityDomainMapper::toAiAnalysisDomain)
    }

    override fun findById(id: Long): AiAnalysis? {
        return aiAnalysisRepository.findById(id)
            .map(entityDomainMapper::toAiAnalysisDomain)
            .orElse(null)
    }

    override fun findValidByDocumentId(documentId: Long): List<AiAnalysis> {
        return aiAnalysisRepository.findByDocumentIdAndIsValidTrueOrderByCreatedAtDesc(documentId)
            .map(entityDomainMapper::toAiAnalysisDomain)
    }

    override fun findLatestValidByDocumentId(documentId: Long): AiAnalysis? {
        return aiAnalysisRepository.findFirstByDocumentIdAndIsValidTrueOrderByCreatedAtDesc(documentId)
            ?.let(entityDomainMapper::toAiAnalysisDomain)
    }

    override fun findAllByDocumentId(documentId: Long): List<AiAnalysis> {
        return aiAnalysisRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
            .map(entityDomainMapper::toAiAnalysisDomain)
    }

    override fun hasValidAnalyses(documentId: Long): Boolean {
        return aiAnalysisRepository.existsByDocumentIdAndIsValidTrue(documentId)
    }

    override fun markAsInvalid(id: Long): Boolean {
        return try {
            val rowsUpdated = aiAnalysisRepository.invalidateAnalysis(id)
            rowsUpdated > 0
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteById(id: Long) {
        aiAnalysisRepository.deleteById(id)
    }

    override fun deleteAllByDocumentId(documentId: Long) {
        aiAnalysisRepository.deleteByDocumentId(documentId)
    }

    override fun count(): Long {
        return aiAnalysisRepository.count()
    }

    override fun countValid(): Long {
        return aiAnalysisRepository.countByIsValidTrue()
    }
}