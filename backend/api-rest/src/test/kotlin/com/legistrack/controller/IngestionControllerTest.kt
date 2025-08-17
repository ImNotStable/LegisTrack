package com.legistrack.controller

import com.legistrack.domain.entity.IngestionRun
import com.legistrack.domain.port.IngestionRunRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.time.LocalDateTime

class IngestionControllerTest {
    private val repo: IngestionRunRepositoryPort = mockk()
    private val controller = IngestionController(repo)

    @Test
    fun should_returnStatusDto_withRates() {
        val now = LocalDateTime.now()
        val from = LocalDate.now().minusDays(7)
        val latest = IngestionRun(id=1, fromDate=from, status=IngestionRun.Status.IN_PROGRESS, startedAt=now)
        val success = IngestionRun(id=2, fromDate=from, status=IngestionRun.Status.SUCCESS, startedAt=now.minusMinutes(10), completedAt=now.minusMinutes(5), documentCount=12)
        val failure = IngestionRun(id=3, fromDate=from, status=IngestionRun.Status.FAILURE, startedAt=now.minusHours(2), completedAt=now.minusHours(1), errorMessage="boom")
        every { repo.findLatestRun() } returns latest
        every { repo.findLatestSuccessful() } returns success
        every { repo.findLatestFailure() } returns failure

        val response = controller.getStatus()
        val body = response.body!!
        assertEquals(2, body.latestSuccess?.id)
        assertEquals(3, body.latestFailure?.id)
        assertEquals(1, body.latestRun?.id)
        assertEquals(0.5, body.successRate) // 1 success / (1 success + 1 failure)
    }
}
