package com.legistrack.persistence.mapper

import com.legistrack.domain.entity.AiAnalysis as DomainAiAnalysis
import com.legistrack.domain.entity.Document as DomainDocument
import com.legistrack.domain.entity.DocumentAction as DomainDocumentAction
import com.legistrack.domain.entity.DocumentSponsor as DomainDocumentSponsor
import com.legistrack.domain.entity.Sponsor as DomainSponsor
import com.legistrack.persistence.entity.AiAnalysis as JpaAiAnalysis
import com.legistrack.persistence.entity.Document as JpaDocument
import com.legistrack.persistence.entity.DocumentAction as JpaDocumentAction
import com.legistrack.persistence.entity.DocumentSponsor as JpaDocumentSponsor
import com.legistrack.persistence.entity.Sponsor as JpaSponsor
import org.springframework.stereotype.Component

/**
 * Mapper for converting between JPA entities and domain entities.
 *
 * Handles the impedance mismatch between persistence layer (with JPA relationships)
 * and domain layer (with ID references only).
 */
@Component
class EntityDomainMapper {

    /**
     * Converts JPA Document entity to domain Document entity.
     */
    fun toDomainEntity(jpaDocument: JpaDocument): DomainDocument = DomainDocument(
        id = jpaDocument.id,
        billId = jpaDocument.billId,
        title = jpaDocument.title,
        officialSummary = jpaDocument.officialSummary,
        introductionDate = jpaDocument.introductionDate,
        congressSession = jpaDocument.congressSession,
        billType = jpaDocument.billType,
        fullTextUrl = jpaDocument.fullTextUrl,
        status = jpaDocument.status,
        createdAt = jpaDocument.createdAt,
        updatedAt = jpaDocument.updatedAt,
        sponsorIds = jpaDocument.sponsors.mapNotNull { it.id },
        actionIds = jpaDocument.actions.mapNotNull { it.id },
        analysisIds = jpaDocument.analyses.mapNotNull { it.id }
    )

    /**
     * Converts domain Document entity to JPA Document entity.
     */
    fun toJpaEntity(domainDocument: DomainDocument): JpaDocument = JpaDocument(
        id = domainDocument.id,
        billId = domainDocument.billId,
        title = domainDocument.title,
        officialSummary = domainDocument.officialSummary,
        introductionDate = domainDocument.introductionDate,
        congressSession = domainDocument.congressSession,
        billType = domainDocument.billType,
        fullTextUrl = domainDocument.fullTextUrl,
        status = domainDocument.status,
        createdAt = domainDocument.createdAt,
        updatedAt = domainDocument.updatedAt
        // Note: relationships are not set here - they need to be managed separately
    )

    /**
     * Converts JPA Sponsor entity to domain Sponsor entity.
     */
    fun toDomainSponsor(jpaSponsor: JpaSponsor): DomainSponsor = DomainSponsor(
        id = jpaSponsor.id,
        bioguideId = jpaSponsor.bioguideId,
        firstName = jpaSponsor.firstName,
        lastName = jpaSponsor.lastName,
        party = jpaSponsor.party,
        state = jpaSponsor.state,
        district = jpaSponsor.district,
        createdAt = jpaSponsor.createdAt,
        updatedAt = jpaSponsor.updatedAt,
        documentSponsorIds = jpaSponsor.documentSponsors.mapNotNull { it.id }
    )

    /**
     * Converts domain Sponsor entity to JPA Sponsor entity.
     */
    fun toJpaSponsor(domainSponsor: DomainSponsor): JpaSponsor = JpaSponsor(
        id = domainSponsor.id,
        bioguideId = domainSponsor.bioguideId,
        firstName = domainSponsor.firstName,
        lastName = domainSponsor.lastName,
        party = domainSponsor.party,
        state = domainSponsor.state,
        district = domainSponsor.district,
        createdAt = domainSponsor.createdAt,
        updatedAt = domainSponsor.updatedAt
    )

    /**
     * Converts JPA DocumentSponsor entity to domain DocumentSponsor entity.
     */
    fun toDomainDocumentSponsor(jpaDocumentSponsor: JpaDocumentSponsor): DomainDocumentSponsor = DomainDocumentSponsor(
        id = jpaDocumentSponsor.id,
        documentId = jpaDocumentSponsor.document.id ?: 0L,
        sponsorId = jpaDocumentSponsor.sponsor.id ?: 0L,
        isPrimarySponsor = jpaDocumentSponsor.isPrimarySponsor,
        sponsorDate = jpaDocumentSponsor.sponsorDate,
        createdAt = jpaDocumentSponsor.createdAt
    )

    /**
     * Converts JPA DocumentAction entity to domain DocumentAction entity.
     */
    fun toDomainDocumentAction(jpaDocumentAction: JpaDocumentAction): DomainDocumentAction = DomainDocumentAction(
        id = jpaDocumentAction.id,
        documentId = jpaDocumentAction.document.id ?: 0L,
        actionDate = jpaDocumentAction.actionDate,
        actionType = jpaDocumentAction.actionType,
        actionText = jpaDocumentAction.actionText,
        chamber = jpaDocumentAction.chamber,
        actionCode = jpaDocumentAction.actionCode,
        createdAt = jpaDocumentAction.createdAt
    )

    /**
     * Converts JPA AiAnalysis entity to domain AiAnalysis entity.
     */
    fun toDomainAiAnalysis(jpaAiAnalysis: JpaAiAnalysis): DomainAiAnalysis = DomainAiAnalysis(
        id = jpaAiAnalysis.id,
        documentId = jpaAiAnalysis.document.id ?: 0L,
        generalEffectText = jpaAiAnalysis.generalEffectText,
        economicEffectText = jpaAiAnalysis.economicEffectText,
        industryTags = jpaAiAnalysis.industryTags.toList(),
        isValid = jpaAiAnalysis.isValid,
        analysisDate = jpaAiAnalysis.analysisDate,
        modelUsed = jpaAiAnalysis.modelUsed,
        createdAt = jpaAiAnalysis.createdAt,
        updatedAt = jpaAiAnalysis.updatedAt
    )

    /**
     * Converts domain AiAnalysis entity to JPA AiAnalysis entity.
     * Requires a JPA document to establish the relationship.
     */
    fun toJpaAiAnalysis(domainAiAnalysis: DomainAiAnalysis, jpaDocument: JpaDocument): JpaAiAnalysis = JpaAiAnalysis(
        id = domainAiAnalysis.id,
        document = jpaDocument,
        generalEffectText = domainAiAnalysis.generalEffectText,
        economicEffectText = domainAiAnalysis.economicEffectText,
        industryTags = domainAiAnalysis.industryTags.toTypedArray(),
        isValid = domainAiAnalysis.isValid,
        analysisDate = domainAiAnalysis.analysisDate,
        modelUsed = domainAiAnalysis.modelUsed,
        createdAt = domainAiAnalysis.createdAt,
        updatedAt = domainAiAnalysis.updatedAt
    )

    /**
     * Converts domain AiAnalysis entity to JPA AiAnalysis entity.
     * This is a simplified version for the adapter pattern.
     */
    fun toAiAnalysisEntity(domainAiAnalysis: DomainAiAnalysis): JpaAiAnalysis {
        // Create a stub document with just the ID for the relationship
        val stubDocument = JpaDocument(
            id = domainAiAnalysis.documentId,
            billId = "", // stub value
            title = "" // stub value
        )
        return toJpaAiAnalysis(domainAiAnalysis, stubDocument)
    }

    /**
     * Converts JPA AiAnalysis entity to domain AiAnalysis entity.
     * This is an alias for consistency with the adapter pattern.
     */
    fun toAiAnalysisDomain(jpaAiAnalysis: JpaAiAnalysis): DomainAiAnalysis = toDomainAiAnalysis(jpaAiAnalysis)
}