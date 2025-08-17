package com.legistrack.controller

import com.legistrack.domain.entity.IngestionRun
import com.legistrack.domain.port.IngestionRunRepositoryPort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/ingestion")
class IngestionController(
    private val ingestionRunRepositoryPort: IngestionRunRepositoryPort,
) {
    data class IngestionStatusDto(
        val latestRun: RunDto?,
        val latestSuccess: RunDto?,
        val latestFailure: RunDto?,
        val successRate: Double,
    )
    data class RunDto(
        val id: Long?,
        val fromDate: String,
        val status: String,
        val startedAtEpochMs: Long,
        val completedAtEpochMs: Long?,
        val documentCount: Int,
        val errorMessage: String?,
    )

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<IngestionStatusDto> {
        val latest = ingestionRunRepositoryPort.findLatestRun()
        val success = ingestionRunRepositoryPort.findLatestSuccessful()
        val failure = ingestionRunRepositoryPort.findLatestFailure()
        val total = listOfNotNull(
            success?.let { 1 },
            failure?.let { 1 },
        ).size.toDouble().coerceAtLeast(0.0)
        val successRate = if (total == 0.0) 0.0 else 1.0 * (if (success != null) 1 else 0) / total
        val dto = IngestionStatusDto(
            latestRun = latest?.toDto(),
            latestSuccess = success?.toDto(),
            latestFailure = failure?.toDto(),
            successRate = successRate,
        )
        return ResponseEntity.ok(dto)
    }

    private fun IngestionRun.toDto(): RunDto = RunDto(
        id = id,
        fromDate = fromDate.toString(),
        status = status.name,
        startedAtEpochMs = startedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
        completedAtEpochMs = completedAt?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
        documentCount = documentCount,
        errorMessage = errorMessage,
    )
}
