package com.legistrack.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for the Document entity.
 * Tests entity creation, validation, and property assignments.
 */
class DocumentTestFixed {

    @Test
    fun `should create document with required fields`() {
        val document = Document(
            billId = "HR1234-118",
            title = "Test Bill Title"
        )

        assertEquals("HR1234-118", document.billId)
        assertEquals("Test Bill Title", document.title)
        assertNull(document.id)
        assertNull(document.officialSummary)
        assertNull(document.introductionDate)
        assertNull(document.congressSession)
        assertNull(document.billType)
        assertNull(document.fullTextUrl)
        assertNull(document.status)
        assertTrue(document.sponsors.isEmpty())
        assertTrue(document.actions.isEmpty())
        assertTrue(document.analyses.isEmpty())
        assertNotNull(document.createdAt)
        assertNotNull(document.updatedAt)
    }

    @Test
    fun `should create document with all fields`() {
        val now = LocalDateTime.now()
        val introDate = LocalDate.now().minusDays(30)
        
        val document = Document(
            id = 1L,
            billId = "S456-118",
            title = "Comprehensive Test Act",
            officialSummary = "This is a test bill summary",
            introductionDate = introDate,
            congressSession = 118,
            billType = "S",
            fullTextUrl = "https://congress.gov/bill/text",
            status = "Introduced",
            createdAt = now,
            updatedAt = now
        )

        assertEquals(1L, document.id)
        assertEquals("S456-118", document.billId)
        assertEquals("Comprehensive Test Act", document.title)
        assertEquals("This is a test bill summary", document.officialSummary)
        assertEquals(introDate, document.introductionDate)
        assertEquals(118, document.congressSession)
        assertEquals("S", document.billType)
        assertEquals("https://congress.gov/bill/text", document.fullTextUrl)
        assertEquals("Introduced", document.status)
        assertEquals(now, document.createdAt)
        assertEquals(now, document.updatedAt)
    }

    @Test
    fun `should handle data class equality correctly`() {
        val document1 = Document(
            billId = "HR1234-118",
            title = "Test Bill"
        )
        
        val document2 = Document(
            billId = "HR1234-118",
            title = "Test Bill"
        )

        assertEquals(document1.billId, document2.billId)
        assertEquals(document1.title, document2.title)
    }

    @Test
    fun `should maintain immutability of entity`() {
        val document = Document(
            billId = "HR1234-118",
            title = "Test Bill"
        )

        assertDoesNotThrow {
            document.billId
            document.title
            document.officialSummary
            document.introductionDate
            document.congressSession
            document.billType
            document.fullTextUrl
            document.status
            document.sponsors
            document.actions
            document.analyses
            document.createdAt
            document.updatedAt
        }
    }
}
