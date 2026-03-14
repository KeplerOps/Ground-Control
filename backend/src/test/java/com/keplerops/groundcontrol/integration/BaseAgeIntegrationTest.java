package com.keplerops.groundcontrol.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Tag("age")
public abstract class BaseAgeIntegrationTest {

    static final PostgreSQLContainer<?> agePostgres;

    static {
        agePostgres = new PostgreSQLContainer<>("apache/age:release_PG16_1.6.0");
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
