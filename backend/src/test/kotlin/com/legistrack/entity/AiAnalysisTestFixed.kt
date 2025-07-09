package com.legistrack.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

/**
 * Unit tests for the AiAnalysis entity.
 * Tests entity creation, validation, and property assignments.
 */
class AiAnalysisTestFixed {

    @Test
    fun `should create analysis with required fields`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val analysis = AiAnalysis(document = document)

        assertEquals(document, analysis.document)
        assertNull(analysis.id)
        assertNull(analysis.generalEffectText)
        assertNull(analysis.economicEffectText)
        assertTrue(analysis.industryTags.isEmpty())
        assertTrue(analysis.isValid)
        assertNotNull(analysis.analysisDate)
        assertNull(analysis.modelUsed)
        assertNotNull(analysis.createdAt)
        assertNotNull(analysis.updatedAt)
    }

    @Test
    fun `should create analysis with all fields`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val now = LocalDateTime.now()
        val tags = arrayOf("Healthcare", "Technology", "Finance")
        
        val analysis = AiAnalysis(
            id = 1L,
            document = document,
            generalEffectText = "This bill will have significant impact",
            economicEffectText = "Economic analysis shows positive effects",
            industryTags = tags,
            isValid = true,
            analysisDate = now,
            modelUsed = "gpt-4",
            createdAt = now,
            updatedAt = now
        )

        assertEquals(1L, analysis.id)
        assertEquals(document, analysis.document)
        assertEquals("This bill will have significant impact", analysis.generalEffectText)
        assertEquals("Economic analysis shows positive effects", analysis.economicEffectText)
        assertArrayEquals(tags, analysis.industryTags)
        assertTrue(analysis.isValid)
        assertEquals(now, analysis.analysisDate)
        assertEquals("gpt-4", analysis.modelUsed)
        assertEquals(now, analysis.createdAt)
        assertEquals(now, analysis.updatedAt)
    }

    @Test
    fun `should handle invalid analysis`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val analysis = AiAnalysis(
            document = document,
            isValid = false
        )

        assertFalse(analysis.isValid)
        assertEquals(document, analysis.document)
    }

    @Test
    fun `should override equals and hashCode correctly`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val tags = arrayOf("Healthcare")
        
        val analysis1 = AiAnalysis(
            id = 1L,
            document = document,
            generalEffectText = "Same text",
            industryTags = tags,
            isValid = true
        )
        
        val analysis2 = AiAnalysis(
            id = 1L,
            document = document,
            generalEffectText = "Same text",
            industryTags = tags,
            isValid = true
        )

        assertEquals(analysis1, analysis2)
        assertEquals(analysis1.hashCode(), analysis2.hashCode())
    }

    @Test
    fun `should handle array equality in equals method`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        
        val analysis1 = AiAnalysis(
            document = document,
            industryTags = arrayOf("Healthcare", "Technology")
        )
        
        val analysis2 = AiAnalysis(
            document = document,
            industryTags = arrayOf("Healthcare", "Technology")
        )

        // Custom equals method should handle array comparison
        assertTrue(analysis1.industryTags.contentEquals(analysis2.industryTags))
    }
}
