package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enums.SessionStatus;
import com.lz_insurance.insurance.identity.domain.model.SessionRegistry;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SessionRegistryEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.SessionRegistryJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.TenantScopedSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Import({SessionRegistryRepositoryAdapter.class, SessionRegistryPersistenceMapperImpl.class})
@DisplayName("SessionRegistryRepositoryAdapter — persistence round-trip (tenant-scoped)")
class SessionRegistryRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private SessionRegistryRepositoryAdapter adapter;

    @Autowired
    private SessionRegistryJpaRepository jpaRepository;

    private SessionRegistry newSession(String profileId, String tenantId, String token) {
        return new SessionRegistry(profileId, tenantId, token, "127.0.0.1", "JUnit",
                LocalDateTime.now().plusHours(1));
    }

    @Test
    @DisplayName("save then reload preserves id and starts ACTIVE")
    void saveThenReload_preservesIdentityAndAudit() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        String token = "tok-" + uniq();
        SessionRegistry saved = adapter.save(newSession(profileId, tenantId, token));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        SessionRegistry loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getIdentityProfileId()).isEqualTo(profileId);
        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("revoke increments version and terminates the session")
    void revoke_incrementsVersionAndTerminates() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        SessionRegistry saved = adapter.save(newSession(profileId, tenantId, "tok-" + uniq()));
        flushAndClear();

        SessionRegistry loaded = adapter.findById(saved.getId()).orElseThrow();
        loaded.revoke();
        adapter.save(loaded);
        flushAndClear();

        SessionRegistry reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1L);
        assertThat(reloaded.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(reloaded.getUpdatedBy()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("findBySessionToken returns the matching session")
    void findBySessionToken_returnsMatch() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        String token = "tok-" + uniq();
        adapter.save(newSession(profileId, tenantId, token));
        flushAndClear();

        assertThat(adapter.findBySessionToken(token)).isPresent();
        assertThat(adapter.findBySessionToken("missing-token")).isEmpty();
    }

    @Test
    @DisplayName("TenantScopedSpecification filters by tenant")
    void tenantScopedSpecification_filtersByTenant() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        adapter.save(newSession(profileId, tenantId, "tok-" + uniq()));
        flushAndClear();

        Specification<SessionRegistryEntity> inTenant =
                new TenantScopedSpecification<SessionRegistryEntity>().withTenantId(tenantId).notDeleted();
        assertThat(jpaRepository.findAll(inTenant)).hasSize(1);

        Specification<SessionRegistryEntity> otherTenant =
                new TenantScopedSpecification<SessionRegistryEntity>().withTenantId("no-such-tenant").notDeleted();
        assertThat(jpaRepository.findAll(otherTenant)).isEmpty();
    }
}
