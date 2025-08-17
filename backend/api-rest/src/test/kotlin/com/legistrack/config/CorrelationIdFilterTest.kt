package com.legistrack.config

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals

class CorrelationIdFilterTest {
    private val filter = CorrelationIdFilter()

    @Test
    fun `adds correlation id when absent`() {
        val req = MockHttpServletRequest("GET", "/api/documents")
        val resp = MockHttpServletResponse()
        filter.doFilter(req, resp, MockFilterChain())
        val header = resp.getHeader(CorrelationIdFilter.HEADER)
    assertNotNull(header)
    assertEquals(36, header!!.length) // UUID length
    }

    @Test
    fun `propagates existing correlation id`() {
        val req = MockHttpServletRequest("GET", "/api/documents")
        val original = "12345678-1234-1234-1234-1234567890ab"
        req.addHeader(CorrelationIdFilter.HEADER, original)
        val resp = MockHttpServletResponse()
        filter.doFilter(req, resp, MockFilterChain())
        val header = resp.getHeader(CorrelationIdFilter.HEADER)
        assertEquals(original, header)
    }
}
