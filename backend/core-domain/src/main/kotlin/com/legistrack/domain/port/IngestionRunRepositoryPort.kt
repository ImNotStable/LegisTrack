package com.legistrack.domain.port

import com.legistrack.domain.entity.IngestionRun
import java.time.LocalDate

/**
 * Port for ingestion run ledger persistence.
 */
interface IngestionRunRepositoryPort {
    fun create(fromDate: LocalDate): IngestionRun?
    fun findSuccessful(fromDate: LocalDate): IngestionRun?
    fun markSuccess(id: Long, documentCount: Int): IngestionRun?
    fun markFailure(id: Long, errorMessage: String?): IngestionRun?
    fun findLatestRun(): IngestionRun?
    fun findLatestSuccessful(): IngestionRun?
    fun findLatestFailure(): IngestionRun?
}
