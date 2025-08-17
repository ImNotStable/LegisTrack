package com.legistrack.aianalysis

import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.AiAnalysisRepositoryPort
import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.port.DocumentRepositoryPort
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AiAnalysisServiceMetricsTest {

    private fun newService(
        aiReady: Boolean = true,
        general: String? = "General text",
        economic: String? = "Economic text",
        tags: List<String> = listOf("A", "B"),
        persistSuccess: Boolean = true,
    ): AiAnalysisService {
        val aiModel = mockk<AiModelPort>()
        every { aiModel.isServiceReady() } returns aiReady
        coEvery { aiModel.generateAnalysis(match { it.startsWith("Provide a concise") }) } returns general
        coEvery { aiModel.generateAnalysis(match { it.startsWith("Analyze potential economic") }) } returns economic
        coEvery { aiModel.generateAnalysis(match { it.startsWith("List up to") }) } returns tags.joinToString(",")
        val repo = mockk<AiAnalysisRepositoryPort>()
        if (persistSuccess) {
            coEvery { repo.save(any()) } answers { firstArg() }
        } else {
            coEvery { repo.save(any()) } throws RuntimeException("db fail")
        }
        val docRepo = mockk<DocumentRepositoryPort>()
        val registry = SimpleMeterRegistry()
        return AiAnalysisService(aiModel, repo, docRepo, registry)
    }

    private fun baseDocument() = Document(
        id = 1L,
        billId = "HR1234-118",
        title = "Sample",
        officialSummary = "Summary",
        introductionDate = null,
        congressSession = 118,
        billType = "HR",
        fullTextUrl = null,
        status = "INTRODUCED",
    )

    @Test
    fun `metrics increment on successful generation`() = runBlocking {
        val service = newService()
        val doc = baseDocument()
        val result = service.generateAndPersist(doc)
        assertThat(result).isNotNull()
        val registry = service.javaClass.getDeclaredField("meterRegistry").let { f ->
            f.isAccessible = true; f.get(service) as SimpleMeterRegistry
        }
        assertThat(registry.counter("ai.analysis.requests").count()).isEqualTo(1.0)
        assertThat(registry.counter("ai.analysis.failures").count()).isEqualTo(0.0)
        assertThat(registry.counter("ai.analysis.prompt.chars").count()).isGreaterThan(0.0)
        assertThat(registry.counter("ai.analysis.response.chars").count()).isGreaterThan(0.0)
        assertThat(registry.timer("ai.analysis.latency").count()).isEqualTo(1)
    }

    @Test
    fun `metrics reflect failure path`() = runBlocking {
        val service = newService(persistSuccess = false)
        val doc = baseDocument()
        val result = service.generateAndPersist(doc)
        assertThat(result).isNull()
        val registry = service.javaClass.getDeclaredField("meterRegistry").let { f ->
            f.isAccessible = true; f.get(service) as SimpleMeterRegistry
        }
        assertThat(registry.counter("ai.analysis.requests").count()).isEqualTo(1.0)
        assertThat(registry.counter("ai.analysis.failures").count()).isGreaterThanOrEqualTo(1.0)
    }
}
