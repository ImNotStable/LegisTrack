package com.legistrack.aianalysis

import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.AiAnalysisRepositoryPort
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.port.DocumentRepositoryPort
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AiAnalysisGaugesTest {
    private val modelPort: AiModelPort = mockk()
    private val repoPort: AiAnalysisRepositoryPort = mockk()
    private val docPort: DocumentRepositoryPort = mockk()
    private val registry = SimpleMeterRegistry()

    @Test
    fun should_update_gauges_on_success_and_failure() = runBlocking {
    coEvery { modelPort.isServiceReady() } returns true
    coEvery { modelPort.isModelAvailable() } returns true
        coEvery { modelPort.generateAnalysis(any()) } returnsMany listOf("general text", "economic text", "tag1,tag2")
        coEvery { repoPort.save(any()) } returns mockk(relaxed = true)
        val document = Document(id = 1L, billId = "H.R.1-118", title = "Test Bill", officialSummary = "Summary", createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        val service = AiAnalysisService(modelPort, repoPort, docPort, registry)

        service.generateAndPersist(document)

        val successRate = registry.find("ai.analysis.success.rate").gauge()?.value()
        val lastChars = registry.find("ai.analysis.last.response.chars").gauge()?.value()
        assertThat(successRate).isNotNull()
        assertThat(successRate!!).isEqualTo(1.0)
        assertThat(lastChars).isNotNull()
        assertThat(lastChars!!).isGreaterThan(0.0)
    }
}
