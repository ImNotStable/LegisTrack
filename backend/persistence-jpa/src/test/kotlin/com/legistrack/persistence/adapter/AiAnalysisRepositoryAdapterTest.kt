package com.legistrack.persistence.adapter

import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.persistence.entity.AiAnalysis as JpaAiAnalysis
import com.legistrack.persistence.entity.Document as JpaDocument
import com.legistrack.persistence.mapper.EntityDomainMapper
import com.legistrack.persistence.repository.AiAnalysisRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class AiAnalysisRepositoryAdapterTest {

    private val aiAnalysisRepository: AiAnalysisRepository = mockk()
    private val entityDomainMapper: EntityDomainMapper = mockk()
    private lateinit var adapter: AiAnalysisRepositoryAdapter

    @BeforeEach
    fun setUp() {
        adapter = AiAnalysisRepositoryAdapter(aiAnalysisRepository, entityDomainMapper)
    }

    @Test
    fun `save should convert domain to entity, save, and convert back`() {
        // Given
        val domainAnalysis = createDomainAnalysis()
        val jpaEntity = createJpaEntity()
        val savedJpaEntity = jpaEntity.copy(id = 1L)
        val savedDomainAnalysis = domainAnalysis.copy(id = 1L)

        every { entityDomainMapper.toAiAnalysisEntity(domainAnalysis) } returns jpaEntity
        every { aiAnalysisRepository.save(jpaEntity) } returns savedJpaEntity
        every { entityDomainMapper.toAiAnalysisDomain(savedJpaEntity) } returns savedDomainAnalysis

        // When
        val result = adapter.save(domainAnalysis)

        // Then
        assertEquals(savedDomainAnalysis, result)
        verify { entityDomainMapper.toAiAnalysisEntity(domainAnalysis) }
        verify { aiAnalysisRepository.save(jpaEntity) }
        verify { entityDomainMapper.toAiAnalysisDomain(savedJpaEntity) }
    }

    @Test
    fun `findById should return domain analysis when found`() {
        // Given
        val id = 1L
        val jpaEntity = createJpaEntity().copy(id = id)
        val domainAnalysis = createDomainAnalysis().copy(id = id)

        every { aiAnalysisRepository.findById(id) } returns Optional.of(jpaEntity)
        every { entityDomainMapper.toAiAnalysisDomain(jpaEntity) } returns domainAnalysis

        // When
        val result = adapter.findById(id)

        // Then
        assertEquals(domainAnalysis, result)
        verify { aiAnalysisRepository.findById(id) }
        verify { entityDomainMapper.toAiAnalysisDomain(jpaEntity) }
    }

    @Test
    fun `findById should return null when not found`() {
        // Given
        val id = 1L
        every { aiAnalysisRepository.findById(id) } returns Optional.empty()

        // When
        val result = adapter.findById(id)

        // Then
        assertNull(result)
        verify { aiAnalysisRepository.findById(id) }
    }

    @Test
    fun `findValidByDocumentId should return mapped analyses`() {
        // Given
        val documentId = 100L
        val jpaEntities = listOf(createJpaEntity().copy(id = 1L), createJpaEntity().copy(id = 2L))
        val domainAnalyses = listOf(createDomainAnalysis().copy(id = 1L), createDomainAnalysis().copy(id = 2L))

        every { aiAnalysisRepository.findByDocumentIdAndIsValidTrueOrderByCreatedAtDesc(documentId) } returns jpaEntities
        every { entityDomainMapper.toAiAnalysisDomain(jpaEntities[0]) } returns domainAnalyses[0]
        every { entityDomainMapper.toAiAnalysisDomain(jpaEntities[1]) } returns domainAnalyses[1]

        // When
        val result = adapter.findValidByDocumentId(documentId)

        // Then
        assertEquals(domainAnalyses, result)
        verify { aiAnalysisRepository.findByDocumentIdAndIsValidTrueOrderByCreatedAtDesc(documentId) }
    }

    @Test
    fun `hasValidAnalyses should delegate to repository`() {
        // Given
        val documentId = 100L
        every { aiAnalysisRepository.existsByDocumentIdAndIsValidTrue(documentId) } returns true

        // When
        val result = adapter.hasValidAnalyses(documentId)

        // Then
        assertTrue(result)
        verify { aiAnalysisRepository.existsByDocumentIdAndIsValidTrue(documentId) }
    }

    @Test
    fun `markAsInvalid should update entity when found`() {
        // Given
        val id = 1L
        every { aiAnalysisRepository.invalidateAnalysis(id) } returns 1

        // When
        val result = adapter.markAsInvalid(id)

        // Then
        assertTrue(result)
        verify { aiAnalysisRepository.invalidateAnalysis(id) }
    }

    @Test
    fun `markAsInvalid should return false when not found`() {
        // Given
        val id = 1L
        every { aiAnalysisRepository.invalidateAnalysis(id) } returns 0

        // When
        val result = adapter.markAsInvalid(id)

        // Then
        assertFalse(result)
        verify { aiAnalysisRepository.invalidateAnalysis(id) }
    }

    @Test
    fun `count should delegate to repository`() {
        // Given
        every { aiAnalysisRepository.count() } returns 42L

        // When
        val result = adapter.count()

        // Then
        assertEquals(42L, result)
        verify { aiAnalysisRepository.count() }
    }

    @Test
    fun `countValid should delegate to repository`() {
        // Given
        every { aiAnalysisRepository.countByIsValidTrue() } returns 25L

        // When
        val result = adapter.countValid()

        // Then
        assertEquals(25L, result)
        verify { aiAnalysisRepository.countByIsValidTrue() }
    }

    private fun createDomainAnalysis() = AiAnalysis(
        id = null,
        documentId = 100L,
        generalEffectText = "General effect",
        economicEffectText = "Economic effect",
        industryTags = listOf("healthcare", "technology"),
        isValid = true,
        modelUsed = "gpt-oss:20b",
        analysisDate = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private fun createJpaEntity() = JpaAiAnalysis(
        id = null,
        document = JpaDocument(
            id = 100L,
            billId = "HR123-118",
            title = "Test Bill"
        ),
        generalEffectText = "General effect",
        economicEffectText = "Economic effect",
        industryTags = arrayOf("healthcare", "technology"),
        isValid = true,
        modelUsed = "gpt-oss:20b",
        analysisDate = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}