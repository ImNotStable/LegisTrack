package com.legistrack.health

import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.port.CongressPort
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

class HealthCheckServiceAggregationTest {

    private fun baseService(
        jdbcTemplate: JdbcTemplate = mockk(),
        aiModelPort: AiModelPort = mockk(),
        congressPort: CongressPort = mockk(),
        redisTemplate: StringRedisTemplate? = null,
    ): HealthCheckService = HealthCheckService(
        dataSource = mockk<DataSource>(relaxed = true),
        jdbcTemplate = jdbcTemplate,
        aiModelPort = aiModelPort,
        congressPort = congressPort,
        redisTemplate = redisTemplate,
        meterRegistry = null
    )

    @Test
    fun `all components up yields overall UP`() = runBlocking {
        val jdbc = mockk<JdbcTemplate>()
        every { jdbc.queryForObject("SELECT 1", Int::class.java) } returns 1
        val redis = mockk<StringRedisTemplate>()
        val valueOps = mockk<org.springframework.data.redis.core.ValueOperations<String, String>>()
        every { redis.opsForValue() } returns valueOps
        every { valueOps.set(any(), any()) } returns Unit
        every { valueOps.get(any()) } returns "ok"
        val ai = mockk<AiModelPort>()
        every { ai.isServiceReady() } returns true
        coEvery { ai.isModelAvailable() } returns true
        val congress = mockk<CongressPort>()
        coEvery { congress.ping() } returns true
        val service = baseService(jdbcTemplate = jdbc, aiModelPort = ai, congressPort = congress, redisTemplate = redis)
        val agg = service.health()
        assertEquals("UP", agg.status)
        assertEquals(true, agg.success)
    }

    @Test
    fun `redis degraded only yields overall DEGRADED with success true`() = runBlocking {
        val jdbc = mockk<JdbcTemplate>()
        every { jdbc.queryForObject("SELECT 1", Int::class.java) } returns 1
        // Redis throwing set call
        val redis = mockk<StringRedisTemplate>()
        val valueOps = mockk<org.springframework.data.redis.core.ValueOperations<String, String>>()
        every { redis.opsForValue() } returns valueOps
        every { valueOps.set(any(), any()) } throws RuntimeException("redis down")
        val ai = mockk<AiModelPort>()
        every { ai.isServiceReady() } returns true
        coEvery { ai.isModelAvailable() } returns true
        val congress = mockk<CongressPort>()
        coEvery { congress.ping() } returns true
        val service = baseService(jdbcTemplate = jdbc, aiModelPort = ai, congressPort = congress, redisTemplate = redis)
        val agg = service.health()
        assertEquals("DEGRADED", agg.status)
        assertEquals(true, agg.success)
    }

    @Test
    fun `ollama critical down yields overall DOWN`() = runBlocking {
        val jdbc = mockk<JdbcTemplate>()
        every { jdbc.queryForObject("SELECT 1", Int::class.java) } returns 1
        val ai = mockk<AiModelPort>()
        every { ai.isServiceReady() } returns false
        coEvery { ai.isModelAvailable() } returns false
        val congress = mockk<CongressPort>()
        coEvery { congress.ping() } returns true
        val service = baseService(jdbcTemplate = jdbc, aiModelPort = ai, congressPort = congress, redisTemplate = null)
        val agg = service.health()
        assertEquals("DOWN", agg.status)
        assertEquals(false, agg.success)
    }

    @Test
    fun `database down yields overall DOWN`() = runBlocking {
        val jdbc = mockk<JdbcTemplate>()
        every { jdbc.queryForObject("SELECT 1", Int::class.java) } throws RuntimeException("db down")
        val ai = mockk<AiModelPort>()
        every { ai.isServiceReady() } returns true
        coEvery { ai.isModelAvailable() } returns true
        val congress = mockk<CongressPort>()
        coEvery { congress.ping() } returns true
        val service = baseService(jdbcTemplate = jdbc, aiModelPort = ai, congressPort = congress, redisTemplate = null)
        val agg = service.health()
        assertEquals("DOWN", agg.status)
        assertEquals(false, agg.success)
    }

    @Test
    fun `lastSuccessEpochMs populated only for non-DOWN components`() = runBlocking {
        val jdbc = mockk<JdbcTemplate>()
        every { jdbc.queryForObject("SELECT 1", Int::class.java) } returns 1
        val ai = mockk<AiModelPort>()
        every { ai.isServiceReady() } returns false // ollama DOWN
        coEvery { ai.isModelAvailable() } returns false
        val congress = mockk<CongressPort>()
        coEvery { congress.ping() } returns true
        val service = baseService(jdbcTemplate = jdbc, aiModelPort = ai, congressPort = congress, redisTemplate = null)
        val agg = service.health()
        val db = agg.components["database"]!!
        val ollama = agg.components["ollama"]!!
        val congressComp = agg.components["congressApi"]!!
        // database and congress should have lastSuccessEpochMs, ollama should not
        assert(db.lastSuccessEpochMs != null)
        assert(congressComp.lastSuccessEpochMs != null)
        assert(ollama.lastSuccessEpochMs == null)
    }
}
