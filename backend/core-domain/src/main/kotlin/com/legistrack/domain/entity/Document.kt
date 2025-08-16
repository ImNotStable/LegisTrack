package com.legistrack.domain.entity

import com.legistrack.domain.annotation.DomainEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain entity representing a U.S. legislative document.
 *
 * Pure domain model without persistence concerns.
 * 
 * @property id Unique identifier for the document
 * @property billId Congressional bill identifier (e.g., "HR1234-118")
 * @property title Official title of the legislation
 * @property officialSummary Official summary provided by Congress
 * @property introductionDate Date when the bill was introduced
 * @property congressSession Congressional session number
 * @property billType Type of bill (HR, S, etc.)
 * @property fullTextUrl URL to the full text of the document
 * @property status Current legislative status
 * @property createdAt Timestamp when record was created
 * @property updatedAt Timestamp when record was last updated
 * @property sponsorIds List of sponsor IDs associated with this document
 * @property actionIds List of action IDs associated with this document  
 * @property analysisIds List of analysis IDs associated with this document
 */
@DomainEntity
data class Document(
    val id: Long? = null,
    val billId: String,
    val title: String,
    val officialSummary: String? = null,
    val introductionDate: LocalDate? = null,
    val congressSession: Int? = null,
    val billType: String? = null,
    val fullTextUrl: String? = null,
    val status: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val sponsorIds: List<Long> = emptyList(),
    val actionIds: List<Long> = emptyList(),
    val analysisIds: List<Long> = emptyList(),
) {
    /**
     * Creates a copy of this document with updated metadata.
     */
    fun withUpdatedMetadata(
        title: String? = null,
        officialSummary: String? = null,
        status: String? = null,
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): Document = copy(
        title = title ?: this.title,
        officialSummary = officialSummary ?: this.officialSummary,
        status = status ?: this.status,
        updatedAt = updatedAt
    )

    /**
     * Checks if this document is from the current congressional session.
     */
    fun isCurrentSession(currentSession: Int): Boolean = congressSession == currentSession

    /**
     * Checks if this document has analysis records.
     */
    fun hasAnalyses(): Boolean = analysisIds.isNotEmpty()
}