package com.pmp.predictivemaintenance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test for Milestone 2 acceptance criteria:
 *
 * 1. Application context starts successfully with PostgreSQL available.
 * 2. Flyway executes V1__create_initial_schema.sql without errors.
 * 3. Hibernate validates all entity mappings against the physical schema.
 *
 * Prerequisites: PostgreSQL must be running via Docker Compose before running this test.
 *   docker compose -f docker/docker-compose.yml up -d
 */
@SpringBootTest
class PredictiveMaintenanceApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes:
        // - Spring Boot application context started successfully.
        // - Flyway migration (V1__create_initial_schema.sql) executed without errors.
        // - Hibernate validated all JPA entity mappings against the database schema.
    }
}
