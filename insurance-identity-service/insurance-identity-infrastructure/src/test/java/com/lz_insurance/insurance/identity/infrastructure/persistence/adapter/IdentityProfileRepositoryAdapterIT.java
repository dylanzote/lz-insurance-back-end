package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enumeration.IdentityStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.InternalUserType;
import com.lz_insurance.insurance.identity.domain.model.IdentityProfile;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.IdentityProfileEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.IdentityProfileJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.TenantScopedSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({IdentityProfileRepositoryAdapter.class, IdentityProfilePersistenceMapperImpl.class})
@DisplayName("IdentityProfileRepositoryAdapter — persistence round-trip (tenant-scoped)")
class IdentityProfileRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private IdentityProfileRepositoryAdapter adapter;

    @Autowired
    private IdentityProfileJpaRepository jpaRepository;

    private IdentityProfile newInternalProfile(String tenantId, String branchId, String email) {
        return IdentityProfile.internal(tenantId, branchId, InternalUserType.AGENT, email, "First", "Last");
    }

    @Test
    @DisplayName("save then reload preserves id and starts PENDING_APPROVAL")
    void saveThenReload_preservesIdentityAndAudit() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        IdentityProfile saved = adapter.save(newInternalProfile(tenantId, branchId, "agent-" + uniq() + "@x.com"));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        IdentityProfile loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getBranchId()).isEqualTo(branchId);
        assertThat(loaded.getStatus()).isEqualTo(IdentityStatus.PENDING_APPROVAL);
        assertThat(loaded.getKeycloakUserId()).isNull();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("activation increments version and links the Keycloak account")
    void activate_incrementsVersionAndLinksKeycloak() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        IdentityProfile saved = adapter.save(newInternalProfile(tenantId, branchId, "agent-" + uniq() + "@x.com"));
        flushAndClear();

        IdentityProfile loaded = adapter.findById(saved.getId()).orElseThrow();
        String keycloakId = "kc-" + uniq();
        loaded.activate(keycloakId);
        adapter.save(loaded);
        flushAndClear();

        IdentityProfile reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1L);
        assertThat(reloaded.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(reloaded.getKeycloakUserId()).isEqualTo(keycloakId);
        assertThat(reloaded.getUpdatedBy()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("findByKeycloakUserId and findByTenantIdAndEmail resolve the profile")
    void naturalKeyFinders_resolveProfile() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String email = "agent-" + uniq() + "@x.com";
        IdentityProfile saved = adapter.save(newInternalProfile(tenantId, branchId, email));
        flushAndClear();

        IdentityProfile loaded = adapter.findById(saved.getId()).orElseThrow();
        String keycloakId = "kc-" + uniq();
        loaded.activate(keycloakId);
        adapter.save(loaded);
        flushAndClear();

        assertThat(adapter.findByKeycloakUserId(keycloakId)).isPresent();
        assertThat(adapter.findByTenantIdAndEmail(tenantId, email)).isPresent();
        assertThat(adapter.findByTenantIdAndEmail(tenantId, "missing@x.com")).isEmpty();
    }

    @Test
    @DisplayName("TenantScopedSpecification filters by tenant and branch")
    void tenantScopedSpecification_filtersByTenantAndBranch() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        adapter.save(newInternalProfile(tenantId, branchId, "agent-" + uniq() + "@x.com"));
        flushAndClear();

        Specification<IdentityProfileEntity> scoped =
                new TenantScopedSpecification<IdentityProfileEntity>()
                        .withTenantId(tenantId).withBranchId(branchId).notDeleted();
        assertThat(jpaRepository.findAll(scoped)).hasSize(1);

        Specification<IdentityProfileEntity> otherBranch =
                new TenantScopedSpecification<IdentityProfileEntity>()
                        .withTenantId(tenantId).withBranchId("no-such-branch").notDeleted();
        assertThat(jpaRepository.findAll(otherBranch)).isEmpty();
    }
}
