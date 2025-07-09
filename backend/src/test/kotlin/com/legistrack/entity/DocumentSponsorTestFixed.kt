package com.legistrack.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for the DocumentSponsor entity.
 * Tests entity creation, validation, and relationships.
 */
class DocumentSponsorTestFixed {

    @Test
    fun `should create document sponsor with required fields`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val sponsor = Sponsor(bioguideId = "S000123")
        
        val documentSponsor = DocumentSponsor(
            document = document,
            sponsor = sponsor
        )

        assertEquals(document, documentSponsor.document)
        assertEquals(sponsor, documentSponsor.sponsor)
        assertNull(documentSponsor.id)
        assertFalse(documentSponsor.isPrimarySponsor)
        assertNull(documentSponsor.sponsorDate)
        assertNotNull(documentSponsor.createdAt)
    }

    @Test
    fun `should create document sponsor with all fields`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val sponsor = Sponsor(
            bioguideId = "S000123",
            firstName = "John",
            lastName = "Doe",
            party = "D",
            state = "CA"
        )
        val sponsorDate = LocalDate.now()
        val createdAt = LocalDateTime.now()
        
        val documentSponsor = DocumentSponsor(
            id = 1L,
            document = document,
            sponsor = sponsor,
            isPrimarySponsor = true,
            sponsorDate = sponsorDate,
            createdAt = createdAt
        )

        assertEquals(1L, documentSponsor.id)
        assertEquals(document, documentSponsor.document)
        assertEquals(sponsor, documentSponsor.sponsor)
        assertTrue(documentSponsor.isPrimarySponsor)
        assertEquals(sponsorDate, documentSponsor.sponsorDate)
        assertEquals(createdAt, documentSponsor.createdAt)
    }

    @Test
    fun `should handle primary vs co-sponsor distinction`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val primarySponsor = Sponsor(bioguideId = "S000123")
        val coSponsor = Sponsor(bioguideId = "S000456")
        
        val primaryDocumentSponsor = DocumentSponsor(
            document = document,
            sponsor = primarySponsor,
            isPrimarySponsor = true
        )
        
        val coDocumentSponsor = DocumentSponsor(
            document = document,
            sponsor = coSponsor,
            isPrimarySponsor = false
        )

        assertTrue(primaryDocumentSponsor.isPrimarySponsor)
        assertFalse(coDocumentSponsor.isPrimarySponsor)
        assertEquals(primarySponsor, primaryDocumentSponsor.sponsor)
        assertEquals(coSponsor, coDocumentSponsor.sponsor)
    }

    @Test
    fun `should maintain data class properties`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val sponsor = Sponsor(bioguideId = "S000123")
        
        val documentSponsor = DocumentSponsor(
            document = document,
            sponsor = sponsor
        )

        assertDoesNotThrow {
            documentSponsor.id
            documentSponsor.document
            documentSponsor.sponsor
            documentSponsor.isPrimarySponsor
            documentSponsor.sponsorDate
            documentSponsor.createdAt
        }
    }
}
