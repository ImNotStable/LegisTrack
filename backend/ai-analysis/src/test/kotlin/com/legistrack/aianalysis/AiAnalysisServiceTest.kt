package com.legistrack.aianalysis

import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.port.AiAnalysisRepositoryPort
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.DocumentRepositoryPort
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AiAnalysisServiceTest {
    private val aiModelPort = mockk<AiModelPort>()
    private val aiAnalysisRepositoryPort = mockk<AiAnalysisRepositoryPort>()
    private val documentRepositoryPort = mockk<DocumentRepositoryPort>()
    private val service = AiAnalysisService(aiModelPort, aiAnalysisRepositoryPort, documentRepositoryPort)

    @Test
    fun should_generateGeneralEffect_when_serviceReady() = runBlocking {
        every { aiModelPort.isServiceReady() } returns true
        coEvery { aiModelPort.generateAnalysis(any()) } returns "General impact text"
        val result = service.generateGeneralEffect("Bill X", "Summary")
        assertEquals("General impact text", result)
    }

    @Test
    fun should_returnNull_when_serviceNotReady_generalEffect() = runBlocking {
        every { aiModelPort.isServiceReady() } returns false
        val result = service.generateGeneralEffect("Bill X", null)
        assertNull(result)
    }

    @Test
    fun should_generateIndustryTags_when_serviceReady() = runBlocking {
        every { aiModelPort.isServiceReady() } returns true
        coEvery { aiModelPort.generateAnalysis(match { it.contains("List up to") }) } returns "Energy, Finance, Health"
        val tags = service.generateIndustryTags("Bill Y", "Some summary")
        assertEquals(listOf("Energy", "Finance", "Health"), tags)
    }

    @Test
    fun should_returnEmptyTags_when_serviceNotReady() = runBlocking {
        every { aiModelPort.isServiceReady() } returns false
        val tags = service.generateIndustryTags("Bill Y", null)
        assertTrue(tags.isEmpty())
    }

    @Test
    fun should_returnNull_generateAndPersist_when_notReady() = runBlocking {
        every { aiModelPort.isServiceReady() } returns false
        val doc = Document(id = 1L, billId = "HR1-118", title = "Test")
        val result = service.generateAndPersist(doc)
        assertNull(result)
    }

    @Test
    fun should_returnNull_generateAndPersist_when_noContent() = runBlocking {
        every { aiModelPort.isServiceReady() } returns true
        coEvery { aiModelPort.generateAnalysis(match { it.contains("general effect") }) } returns null
        coEvery { aiModelPort.generateAnalysis(match { it.contains("economic impacts") }) } returns null
        coEvery { aiModelPort.generateAnalysis(match { it.contains("industry sectors") }) } returns null
        val doc = Document(id = 2L, billId = "HR2-118", title = "Test 2")
        val result = service.generateAndPersist(doc)
        assertNull(result)
    }

    @Test
    fun should_persistAnalysis_when_contentGenerated() = runBlocking {
        // Given
        every { aiModelPort.isServiceReady() } returns true
        coEvery { aiModelPort.generateAnalysis(match { it.contains("general effect") }) } returns "General analysis"
        coEvery { aiModelPort.generateAnalysis(match { it.contains("economic impacts") }) } returns "Economic analysis"
        coEvery { aiModelPort.generateAnalysis(match { it.contains("industry sectors") }) } returns "Technology, Healthcare"
        
        val savedAnalysis = AiAnalysis(
            id = 1L,
            documentId = 3L,
            generalEffectText = "General analysis",
            economicEffectText = "Economic analysis",
            industryTags = listOf("Technology", "Healthcare"),
            isValid = true,
            modelUsed = "gpt-oss:20b",
            analysisDate = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { aiAnalysisRepositoryPort.save(any()) } returns savedAnalysis

        // When
        val doc = Document(id = 3L, billId = "HR3-118", title = "Test Bill")
        val result = service.generateAndPersist(doc)

        // Then
        assertNotNull(result)
        assertEquals(savedAnalysis, result)
        verify { aiAnalysisRepositoryPort.save(any()) }
    }

    @Test
    fun should_returnNull_when_persistenceFails() = runBlocking {
        // Given
        every { aiModelPort.isServiceReady() } returns true
        coEvery { aiModelPort.generateAnalysis(match { it.contains("general effect") }) } returns "General analysis"
        coEvery { aiModelPort.generateAnalysis(match { it.contains("economic impacts") }) } returns null
        coEvery { aiModelPort.generateAnalysis(match { it.contains("industry sectors") }) } returns null
        
        every { aiAnalysisRepositoryPort.save(any()) } throws RuntimeException("Database error")

        // When
        val doc = Document(id = 4L, billId = "HR4-118", title = "Test Bill")
        val result = service.generateAndPersist(doc)

        // Then
        assertNull(result)
        verify { aiAnalysisRepositoryPort.save(any()) }
    }
}
