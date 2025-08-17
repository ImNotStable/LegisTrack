package com.legistrack.testsupport

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

/**
 * Provides a singleton Testcontainers Postgres instance for integration tests that need real DDL.
 */
@TestConfiguration
class PostgresTestContainerConfig {
    companion object {
        @JvmStatic
        private val container: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("legistrack_test")
            withUsername("test")
            withPassword("test")
            start()
        }
    }

    @Bean
    fun dataSource(): DataSource = DriverManagerDataSource().apply {
        setDriverClassName("org.postgresql.Driver")
        url = container.jdbcUrl
        username = container.username
        password = container.password
    }
}
