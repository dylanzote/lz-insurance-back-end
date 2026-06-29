package com.lz_insurance.insurance.identity.infrastructure.persistence;

import com.lz_Insurance.insurance.identity.domain.enumeration.ActorType;
import com.lz_Insurance.insurance.identity.domain.enumeration.BranchStatus;
import com.lz_Insurance.insurance.identity.domain.enumeration.BranchType;
import com.lz_Insurance.insurance.identity.domain.enumeration.IdentityStatus;
import com.lz_Insurance.insurance.identity.domain.enumeration.InternalUserType;
import com.lz_Insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionAction;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionResource;
import com.lz_Insurance.insurance.identity.domain.enumeration.RoleType;
import com.lz_Insurance.insurance.identity.domain.enumeration.TenantStatus;
import com.lz_Insurance.persistence.config.JpaAuditingConfig;
import com.lz_Insurance.security.core.service.CurrentUserService;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.BranchEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.IdentityProfileEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.PermissionEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SystemRoleEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.BDDMockito.given;

/**
 * Base for identity persistence integration tests. Runs a real Postgres in Testcontainers
 * (schema created by the service's Liquibase changelogs) and limits the context to a JPA
 * slice plus {@link JpaAuditingConfig}.
 *
 * <p><b>G-007:</b> the slice explicitly {@code @Import}s {@link JpaAuditingConfig} so JPA
 * auditing is active — without it, {@code created_at}/{@code created_by} are {@code NOT NULL}
 * and every insert fails. The real {@link CurrentUserService} is not component-scanned in a
 * slice, so it is supplied here as a {@link MockitoBean}, stubbed to {@code "test-user"}.
 *
 * <p>Foreign keys mean child rows need their parents first; the {@code persistX} helpers seed
 * minimal valid parent entities (auditing populates their audit columns via {@code persist}).
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

    private static final AtomicLong SEQ = new AtomicLong();

    @MockitoBean
    protected CurrentUserService currentUserService;

    @Autowired
    protected TestEntityManager entityManager;

    @BeforeEach
    void stubAuditor() {
        given(currentUserService.getCurrentUserId()).willReturn("test-user");
    }

    /** Short collision-free suffix for unique business keys (codes, emails, tokens). */
    protected String uniq() {
        return Long.toString(SEQ.incrementAndGet());
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // --- FK parent fixtures (return the generated id) ---

    protected String persistTenant() {
        TenantEntity tenant = TenantEntity.builder()
                .name("Tenant " + uniq()).code("T-" + uniq()).country("CA")
                .bootstrapped(false).status(TenantStatus.ACTIVE).build();
        entityManager.persist(tenant);
        entityManager.flush();
        return tenant.getId();
    }

    protected String persistBranch(String tenantId) {
        BranchEntity branch = BranchEntity.builder()
                .tenantId(tenantId).parentBranchId(null)
                .name("Branch " + uniq()).code("B-" + uniq())
                .type(BranchType.HEADQUARTER).status(BranchStatus.ACTIVE).build();
        entityManager.persist(branch);
        entityManager.flush();
        return branch.getId();
    }

    protected String persistProfile(String tenantId, String branchId) {
        IdentityProfileEntity profile = IdentityProfileEntity.builder()
                .tenantId(tenantId).branchId(branchId).actorType(ActorType.INTERNAL)
                .internalUserType(InternalUserType.AGENT)
                .email("user-" + uniq() + "@example.com").firstName("First").lastName("Last")
                .status(IdentityStatus.PENDING_APPROVAL).build();
        entityManager.persist(profile);
        entityManager.flush();
        return profile.getId();
    }

    protected String persistSystemRole() {
        SystemRoleEntity role = SystemRoleEntity.builder()
                .name("ROLE_" + uniq()).roleType(RoleType.SYSTEM).description("seed role").build();
        entityManager.persist(role);
        entityManager.flush();
        return role.getId();
    }

    protected String persistPermission() {
        PermissionEntity permission = PermissionEntity.builder()
                .resource(PermissionResource.POLICY).action(PermissionAction.READ)
                .defaultScope(OrganizationScope.OWN).build();
        entityManager.persist(permission);
        entityManager.flush();
        return permission.getId();
    }
}
