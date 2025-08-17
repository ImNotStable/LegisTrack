package com.legistrack.persistence.adapter

import com.legistrack.domain.entity.IngestionRun
import com.legistrack.domain.port.IngestionRunRepositoryPort
import com.legistrack.persistence.mapper.IngestionRunMapper
import com.legistrack.persistence.repository.IngestionRunRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

@Component
open class IngestionRunRepositoryAdapter(
    private val repository: IngestionRunRepository,
) : IngestionRunRepositoryPort {
    private val logger = LoggerFactory.getLogger(IngestionRunRepositoryAdapter::class.java)

    @Transactional
    override fun create(fromDate: LocalDate): IngestionRun? = try {
        val entity = IngestionRunMapper.toEntity(
            IngestionRun(
                fromDate = fromDate,
                status = IngestionRun.Status.IN_PROGRESS,
            )
        )
        IngestionRunMapper.toDomain(repository.save(entity))
    } catch (e: DataIntegrityViolationException) {
        // Likely due to concurrent success uniqueness race
        logger.warn("[IngestionRunRepository] Integrity violation creating run: ${e.message}")
        null
    }

    override fun findSuccessful(fromDate: LocalDate): IngestionRun? =
        repository.findSuccessful(fromDate).firstOrNull()?.let { IngestionRunMapper.toDomain(it) }

    @Transactional
    override fun markSuccess(id: Long, documentCount: Int): IngestionRun? {
        return try {
            val existing = repository.findById(id).orElse(null) ?: return null
            val updated = existing.copy(
                status = IngestionRun.Status.SUCCESS.name,
                documentCount = documentCount,
                completedAt = LocalDateTime.now(),
            )
            IngestionRunMapper.toDomain(repository.save(updated))
        } catch (e: Exception) {
            logger.error("[IngestionRunRepository] Error marking success: ${e.message}", e)
            null
        }
    }

    @Transactional
    override fun markFailure(id: Long, errorMessage: String?): IngestionRun? {
        return try {
            val existing = repository.findById(id).orElse(null) ?: return null
            val updated = existing.copy(
                status = IngestionRun.Status.FAILURE.name,
                errorMessage = errorMessage,
                completedAt = LocalDateTime.now(),
            )
            IngestionRunMapper.toDomain(repository.save(updated))
        } catch (e: Exception) {
            logger.error("[IngestionRunRepository] Error marking failure: ${e.message}", e)
            null
        }
    }

    override fun findLatestRun(): IngestionRun? = repository.findLatest(PageRequest.of(0,1)).firstOrNull()?.let { IngestionRunMapper.toDomain(it) }
    override fun findLatestSuccessful(): IngestionRun? = repository.findLatestSuccess(PageRequest.of(0,1)).firstOrNull()?.let { IngestionRunMapper.toDomain(it) }
    override fun findLatestFailure(): IngestionRun? = repository.findLatestFailure(PageRequest.of(0,1)).firstOrNull()?.let { IngestionRunMapper.toDomain(it) }
}
