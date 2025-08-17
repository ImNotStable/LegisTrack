package com.legistrack.persistence.mapper

import com.legistrack.domain.entity.IngestionRun as DomainIngestionRun
import com.legistrack.persistence.entity.IngestionRun as JpaIngestionRun

object IngestionRunMapper {
    fun toDomain(entity: JpaIngestionRun): DomainIngestionRun = DomainIngestionRun(
        id = entity.id,
        fromDate = entity.fromDate,
        status = DomainIngestionRun.Status.valueOf(entity.status),
        startedAt = entity.startedAt,
        completedAt = entity.completedAt,
        documentCount = entity.documentCount,
        errorMessage = entity.errorMessage,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(domain: DomainIngestionRun): JpaIngestionRun = JpaIngestionRun(
        id = domain.id,
        fromDate = domain.fromDate,
        status = domain.status.name,
        startedAt = domain.startedAt,
        completedAt = domain.completedAt,
        documentCount = domain.documentCount,
        errorMessage = domain.errorMessage,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
    )
}
