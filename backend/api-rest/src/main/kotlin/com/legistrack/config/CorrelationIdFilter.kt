package com.legistrack.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID
import jakarta.servlet.Filter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : Filter {
    companion object { const val HEADER = "X-Correlation-Id"; const val MDC_KEY = "correlationId" }
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpReq = request as HttpServletRequest
        val httpResp = response as HttpServletResponse
        val existing = httpReq.getHeader(HEADER)
        val cid = existing?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        MDC.put(MDC_KEY, cid)
        try {
            httpResp.setHeader(HEADER, cid)
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }
}
