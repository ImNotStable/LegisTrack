package com.legistrack.domain.entity

import com.legistrack.domain.annotation.DomainEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain entity representing the relationship between a document and its sponsors.
 *
 * @property id Unique identifier
 * @property documentId Reference to the document
 * @property sponsorId Reference to the sponsor
 * @property isPrimarySponsor Whether this is the primary sponsor
 * @property sponsorDate Date when sponsorship was added
 * @property createdAt Timestamp when record was created
 */
@DomainEntity
data class DocumentSponsor(
    val id: Long? = null,
    val documentId: Long,
    val sponsorId: Long,
    val isPrimarySponsor: Boolean = false,
    val sponsorDate: LocalDate? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    /**
     * Creates a new primary sponsor relationship.
     */
    fun asPrimarySponsor(): DocumentSponsor = copy(isPrimarySponsor = true)

    /**
     * Creates a new co-sponsor relationship.
     */
    fun asCoSponsor(): DocumentSponsor = copy(isPrimarySponsor = false)
}