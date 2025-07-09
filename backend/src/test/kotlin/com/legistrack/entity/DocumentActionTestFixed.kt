package com.legistrack.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for the DocumentAction entity.
 * Tests entity creation, validation, and property assignments.
 */
class DocumentActionTestFixed {

    @Test
    fun `should create document action with required fields`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val actionDate = LocalDate.now()
        
        val action = DocumentAction(
            document = document,
            actionDate = actionDate,
            actionText = "Bill introduced in House"
        )

        assertEquals(document, action.document)
        assertEquals(actionDate, action.actionDate)
        assertEquals("Bill introduced in House", action.actionText)
        assertNull(action.id)
        assertNull(action.actionType)
        assertNull(action.chamber)
        assertNull(action.actionCode)
        assertNotNull(action.createdAt)
    }

    @Test
    fun `should create document action with all fields`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val actionDate = LocalDate.now()
        val createdAt = LocalDateTime.now()
        
        val action = DocumentAction(
            id = 1L,
            document = document,
            actionDate = actionDate,
            actionType = "Introduction",
            actionText = "Bill introduced in House",
            chamber = "House",
            actionCode = "H11100",
            createdAt = createdAt
        )

        assertEquals(1L, action.id)
        assertEquals(document, action.document)
        assertEquals(actionDate, action.actionDate)
        assertEquals("Introduction", action.actionType)
        assertEquals("Bill introduced in House", action.actionText)
        assertEquals("House", action.chamber)
        assertEquals("H11100", action.actionCode)
        assertEquals(createdAt, action.createdAt)
    }

    @Test
    fun `should handle different chamber values`() {
        val document = Document(billId = "S456-118", title = "Senate Bill")
        val actionDate = LocalDate.now()
        
        val senateAction = DocumentAction(
            document = document,
            actionDate = actionDate,
            actionText = "Bill introduced in Senate",
            chamber = "Senate"
        )

        assertEquals("Senate", senateAction.chamber)
        assertEquals("Bill introduced in Senate", senateAction.actionText)
    }

    @Test
    fun `should maintain data class properties`() {
        val document = Document(billId = "HR1234-118", title = "Test Bill")
        val actionDate = LocalDate.now()
        
        val action = DocumentAction(
            document = document,
            actionDate = actionDate,
            actionText = "Test action"
        )

        assertDoesNotThrow {
            action.id
            action.document
            action.actionDate
            action.actionType
            action.actionText
            action.chamber
            action.actionCode
            action.createdAt
        }
    }
}
