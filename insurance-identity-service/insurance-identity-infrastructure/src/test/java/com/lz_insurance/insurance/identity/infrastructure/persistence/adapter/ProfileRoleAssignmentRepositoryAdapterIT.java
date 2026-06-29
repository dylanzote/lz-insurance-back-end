package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.ProfileRoleAssignment;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.ProfileRoleAssignmentEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.ProfileRoleAssignmentJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.TenantScopedSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({ProfileRoleAssignmentRepositoryAdapter.class, ProfileRoleAssignmentPersistenceMapperImpl.class})
@DisplayName("ProfileRoleAssignmentRepositoryAdapter — persistence round-trip (tenant-scoped)")
class ProfileRoleAssignmentRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private ProfileRoleAssignmentRepositoryAdapter adapter;

    @Autowired
    private ProfileRoleAssignmentJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id, profile and scope")
    void saveThenReload_preservesIdentityAndAudit() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        String roleId = persistSystemRole();
        ProfileRoleAssignment saved = adapter.save(new ProfileRoleAssignment(profileId, roleId, tenantId, branchId));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        ProfileRoleAssignment loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getIdentityProfileId()).isEqualTo(profileId);
        assertThat(loaded.getRoleId()).isEqualTo(roleId);
        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getBranchId()).isEqualTo(branchId);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("findByIdentityProfileId returns the profile's assignments")
    void findByIdentityProfileId_returnsAssignments() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        String roleId = persistSystemRole();
        adapter.save(new ProfileRoleAssignment(profileId, roleId, tenantId, branchId));
        flushAndClear();

        assertThat(adapter.findByIdentityProfileId(profileId)).hasSize(1);
        assertThat(adapter.findByIdentityProfileId("no-such-profile")).isEmpty();
    }

    @Test
    @DisplayName("TenantScopedSpecification filters by tenant and branch")
    void tenantScopedSpecification_filtersByTenantAndBranch() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String profileId = persistProfile(tenantId, branchId);
        String roleId = persistSystemRole();
        adapter.save(new ProfileRoleAssignment(profileId, roleId, tenantId, branchId));
        flushAndClear();

        Specification<ProfileRoleAssignmentEntity> scoped =
                new TenantScopedSpecification<ProfileRoleAssignmentEntity>()
                        .withTenantId(tenantId).withBranchId(branchId).notDeleted();
        assertThat(jpaRepository.findAll(scoped)).hasSize(1);

        Specification<ProfileRoleAssignmentEntity> otherBranch =
                new TenantScopedSpecification<ProfileRoleAssignmentEntity>()
                        .withTenantId(tenantId).withBranchId("no-such-branch").notDeleted();
        assertThat(jpaRepository.findAll(otherBranch)).isEmpty();
    }
}
