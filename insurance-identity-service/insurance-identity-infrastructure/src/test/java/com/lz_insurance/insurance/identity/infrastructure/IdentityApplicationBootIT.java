package com.lz_insurance.insurance.identity.infrastructure;

import com.lz_insurance.insurance.identity.domain.enums.TenantStatus;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.TenantEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.TenantJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-M1-009 boot / exit-gate verification. Boots the FULL identity application context (not a
 * {@code @DataJpaTest} slice) against a real Postgres, proving:
 * <ul>
 *   <li>the context loads and all 13 Liquibase changelogs apply on a fresh database;</li>
 *   <li>JPA auditing is active — an insert succeeds and {@code created_by} is populated. With no
 *       {@code CurrentUserService} in the M1 context the auditor falls back to {@code "SYSTEM"}
 *       (real logged-in-user capture is deferred to M3 — see G-011);</li>
 *   <li>{@code /actuator/health} returns UP and is reachable without authentication (the M1
 *       servlet-security auto-configs are excluded — see the application class).</li>
 * </ul>
 * Testcontainers-backed: requires a running Docker daemon. Run by Maven Failsafe ({@code *IT}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Identity application — full-context boot (US-M1-009 exit gate)")
class IdentityApplicationBootIT {

    @Container
    static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withExposedPorts(5432)
                    .withEnv("POSTGRES_DB", "identity_boot_test")
                    .withEnv("POSTGRES_USER", "test")
                    .withEnv("POSTGRES_PASSWORD", "test")
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getFirstMappedPort() + "/identity_boot_test");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TenantJpaRepository tenantRepository;

    @Test
    @DisplayName("context boots, Liquibase applies, and JPA auditing populates created_by=SYSTEM on insert")
    void contextBootsWithAuditingActive() {
        TenantEntity tenant = TenantEntity.builder()
                .name("Boot Smoke Tenant").code("BOOT-SMOKE")
                .country("CA").bootstrapped(false).status(TenantStatus.ACTIVE)
                .build();

        TenantEntity saved = tenantRepository.saveAndFlush(tenant);

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy())
                .as("auditorProvider falls back to SYSTEM when no CurrentUserService is in the M1 context")
                .isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("/actuator/health returns UP and is reachable without authentication")
    void healthIsUpAndOpen() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
