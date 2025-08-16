package com.legistrack.domain.entity

import com.legistrack.domain.annotation.DomainEntity
import java.time.LocalDateTime

/**
 * Domain entity representing a congressional sponsor.
 *
 * @property id Unique identifier
 * @property bioguideId Biographical Directory of the United States Congress identifier
 * @property firstName Sponsor's first name
 * @property lastName Sponsor's last name
 * @property party Political party affiliation
 * @property state State represented
 * @property district Congressional district (for House members)
 * @property createdAt Timestamp when record was created
 * @property updatedAt Timestamp when record was last updated
 * @property documentSponsorIds List of document sponsorship relationship IDs
 */
@DomainEntity
data class Sponsor(
    val id: Long? = null,
    val bioguideId: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val party: String? = null,
    val state: String? = null,
    val district: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val documentSponsorIds: List<Long> = emptyList(),
) {
    /**
     * Returns the full name of the sponsor.
     */
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { "Unknown" }

    /**
     * Returns a display string with name and party affiliation.
     */
    val displayName: String
        get() = buildString {
            append(fullName)
            party?.let { append(" ($it)") }
        }

    /**
     * Checks if this is a House representative (has district).
     */
    val isHouseRepresentative: Boolean
        get() = !district.isNullOrBlank()

    /**
     * Checks if this is a Senator (no district).
     */
    val isSenator: Boolean
        get() = district.isNullOrBlank()
}