package com.legistrack.domain.entity

import com.legistrack.domain.annotation.DomainEntity
import java.time.LocalDate
import java.time.LocalDateTime

@DomainEntity
/**
 * Domain entity representing a scheduled ingestion run over a date window (fromDate -> now).
 * Used for idempotency (one SUCCESS per fromDate) and operational metrics.
 */
data class IngestionRun(
    val id: Long? = null,
    val fromDate: LocalDate,
    val status: Status,
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val documentCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    enum class Status { IN_PROGRESS, SUCCESS, FAILURE }

    fun markSuccess(count: Int, completedAt: LocalDateTime = LocalDateTime.now()): IngestionRun = copy(
        status = Status.SUCCESS,
        documentCount = count,
        completedAt = completedAt
    )

    fun markFailure(error: String?, completedAt: LocalDateTime = LocalDateTime.now()): IngestionRun = copy(
        status = Status.FAILURE,
        errorMessage = error,
        completedAt = completedAt
    )
}
