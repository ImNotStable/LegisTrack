package com.legistrack.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

/**
 * Unit tests for the Sponsor entity.
 * Tests entity creation, validation, and property assignments.
 */
class SponsorTestFixed {

    @Test
    fun `should create sponsor with required fields`() {
        val sponsor = Sponsor(bioguideId = "S000123")

        assertEquals("S000123", sponsor.bioguideId)
        assertNull(sponsor.id)
        assertNull(sponsor.firstName)
        assertNull(sponsor.lastName)
        assertNull(sponsor.party)
        assertNull(sponsor.state)
        assertNull(sponsor.district)
        assertTrue(sponsor.documentSponsors.isEmpty())
        assertNotNull(sponsor.createdAt)
        assertNotNull(sponsor.updatedAt)
    }

    @Test
    fun `should create sponsor with all fields`() {
        val now = LocalDateTime.now()
        
        val sponsor = Sponsor(
            id = 1L,
            bioguideId = "S000123",
            firstName = "John",
            lastName = "Doe",
            party = "D",
            state = "CA",
            district = "12",
            createdAt = now,
            updatedAt = now
        )

        assertEquals(1L, sponsor.id)
        assertEquals("S000123", sponsor.bioguideId)
        assertEquals("John", sponsor.firstName)
        assertEquals("Doe", sponsor.lastName)
        assertEquals("D", sponsor.party)
        assertEquals("CA", sponsor.state)
        assertEquals("12", sponsor.district)
        assertEquals(now, sponsor.createdAt)
        assertEquals(now, sponsor.updatedAt)
    }

    @Test
    fun `should handle different party affiliations`() {
        val democraticSponsor = Sponsor(
            bioguideId = "D000123",
            party = "D"
        )
        
        val republicanSponsor = Sponsor(
            bioguideId = "R000123",
            party = "R"
        )
        
        val independentSponsor = Sponsor(
            bioguideId = "I000123",
            party = "I"
        )

        assertEquals("D", democraticSponsor.party)
        assertEquals("R", republicanSponsor.party)
        assertEquals("I", independentSponsor.party)
    }

    @Test
    fun `should handle house vs senate members`() {
        val houseMember = Sponsor(
            bioguideId = "H000123",
            state = "CA",
            district = "12"
        )
        
        val senateMember = Sponsor(
            bioguideId = "S000123",
            state = "CA",
            district = null
        )

        assertEquals("12", houseMember.district)
        assertNull(senateMember.district)
        assertEquals("CA", houseMember.state)
        assertEquals("CA", senateMember.state)
    }

    @Test
    fun `should maintain data class properties`() {
        val sponsor = Sponsor(bioguideId = "S000123")

        assertDoesNotThrow {
            sponsor.id
            sponsor.bioguideId
            sponsor.firstName
            sponsor.lastName
            sponsor.party
            sponsor.state
            sponsor.district
            sponsor.documentSponsors
            sponsor.createdAt
            sponsor.updatedAt
        }
    }
}
