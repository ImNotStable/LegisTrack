package com.legistrack.dto

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Basic DTO for Document entity without relationships.
 * Used for scalar queries to avoid MultipleBagFetchException.
 */
data class DocumentBasicDto(
    val id: Long,
    val billId: String,
    val title: String,
    val officialSummary: String?,
    val introductionDate: LocalDate?,
    val congressSession: Int,
    val billType: String,
    val fullTextUrl: String?,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
