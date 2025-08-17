package com.legistrack

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import com.legistrack.testsupport.PostgresTestContainerConfig

@SpringBootTest(classes = [PostgresTestContainerConfig::class])
@ActiveProfiles("test")
class ApiContextLoadTest {
    @Test
    fun contextLoads() {
        // Verifies Spring context boots with api-rest module wiring
    }
}
