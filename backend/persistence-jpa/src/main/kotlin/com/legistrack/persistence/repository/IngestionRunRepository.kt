package com.legistrack.persistence.repository

import com.legistrack.persistence.entity.IngestionRun
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface IngestionRunRepository : JpaRepository<IngestionRun, Long> {
    @Query(
        """
        SELECT ir FROM IngestionRun ir
        WHERE ir.fromDate = :fromDate AND ir.status = 'SUCCESS'
        ORDER BY ir.completedAt DESC
        """
    )
    fun findSuccessful(@Param("fromDate") fromDate: LocalDate): List<IngestionRun>

    @Query("SELECT ir FROM IngestionRun ir ORDER BY ir.startedAt DESC")
    fun findLatest(pageable: org.springframework.data.domain.Pageable): List<IngestionRun>

    @Query("SELECT ir FROM IngestionRun ir WHERE ir.status='SUCCESS' ORDER BY ir.completedAt DESC")
    fun findLatestSuccess(pageable: org.springframework.data.domain.Pageable): List<IngestionRun>

    @Query("SELECT ir FROM IngestionRun ir WHERE ir.status='FAILURE' ORDER BY ir.completedAt DESC")
    fun findLatestFailure(pageable: org.springframework.data.domain.Pageable): List<IngestionRun>
}
