package com.keplerops.groundcontrol.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Tag("age")
public abstract class BaseAgeIntegrationTest {

    static final PostgreSQLContainer<?> agePostgres;

    static {
        // apache/age publishes a postgres-compatible image; declare it explicitly so Testcontainers
        // accepts it as a substitute for the postgres image family.
        agePostgres = new PostgreSQLContainer<>(
                DockerImageName.parse("apache/age:release_PG16_1.6.0").asCompatibleSubstituteFor("postgres"));
        agePostgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", agePostgres::getJdbcUrl);
        registry.add("spring.datasource.username", agePostgres::getUsername);
        registry.add("spring.datasource.password", agePostgres::getPassword);
        registry.add("groundcontrol.age.enabled", () -> "true");
    }
}
