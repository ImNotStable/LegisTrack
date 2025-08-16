package com.legistrack.domain.entity

import com.legistrack.domain.annotation.DomainEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain entity representing a legislative action taken on a document.
 *
 * @property id Unique identifier
 * @property documentId Reference to the document
 * @property actionDate Date when the action occurred
 * @property actionType Type of action taken
 * @property actionText Description of the action
 * @property chamber Chamber where action occurred (House, Senate)
 * @property actionCode Congressional action code
 * @property createdAt Timestamp when record was created
 */
@DomainEntity
data class DocumentAction(
    val id: Long? = null,
    val documentId: Long,
    val actionDate: LocalDate,
    val actionType: String? = null,
    val actionText: String,
    val chamber: String? = null,
    val actionCode: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    /**
     * Checks if this action occurred in the House.
     */
    fun isHouseAction(): Boolean = chamber?.lowercase() == "house"

    /**
     * Checks if this action occurred in the Senate.
     */
    fun isSenateAction(): Boolean = chamber?.lowercase() == "senate"

    /**
     * Checks if this action is recent (within specified days).
     */
    fun isRecent(days: Long): Boolean = 
        actionDate.isAfter(LocalDate.now().minusDays(days))

    /**
     * Returns a formatted display string for this action.
     */
    val displayString: String
        get() = buildString {
            append(actionDate)
            chamber?.let { append(" ($it)") }
            append(": ")
            append(actionText)
        }
}