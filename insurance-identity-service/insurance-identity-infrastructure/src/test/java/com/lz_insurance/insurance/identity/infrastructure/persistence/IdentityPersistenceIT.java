package com.lz_insurance.insurance.identity.infrastructure.persistence;

import com.lz_Insurance.persistence.config.JpaAuditingConfig;
import com.lz_Insurance.security.core.service.CurrentUserService;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for identity persistence integration tests. Runs a real Postgres in Testcontainers
 * (schema created by the service's Liquibase changelogs) and limits the context to a JPA
 * slice plus {@link JpaAuditingConfig}.
 *
 * <p><b>G-007:</b> the slice explicitly {@code @Import}s {@link JpaAuditingConfig} so JPA
 * auditing is active — without it, {@code created_at}/{@code created_by} are {@code NOT NULL}
 * and every insert fails. The real {@link CurrentUserService} is not component-scanned in a
 * slice, so it is supplied here as a {@link MockitoBean} (stub the auditor per test).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
public abstract class IdentityPersistenceIT {

    @Container
    static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withExposedPorts(5432)
                    .withEnv("POSTGRES_DB", "identity_test")
                    .withEnv("POSTGRES_USER", "test")
                    .withEnv("POSTGRES_PASSWORD", "test")
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getFirstMappedPort() + "/identity_test");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

    @MockitoBean
    protected CurrentUserService currentUserService;
}
