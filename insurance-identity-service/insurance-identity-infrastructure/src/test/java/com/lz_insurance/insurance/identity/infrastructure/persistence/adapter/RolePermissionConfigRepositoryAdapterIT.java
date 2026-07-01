package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enums.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.model.RolePermissionConfig;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.RolePermissionConfigEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.RolePermissionConfigJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.TenantScopedSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({RolePermissionConfigRepositoryAdapter.class, RolePermissionConfigPersistenceMapperImpl.class})
@DisplayName("RolePermissionConfigRepositoryAdapter — persistence round-trip (tenant overrides)")
class RolePermissionConfigRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private RolePermissionConfigRepositoryAdapter adapter;

    @Autowired
    private RolePermissionConfigJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id, tenant scope and grant")
    void saveThenReload_preservesIdentityAndAudit() {
        String tenantId = persistTenant();
        String roleId = persistSystemRole();
        String permissionId = seededPermissionId();
        RolePermissionConfig saved = adapter.save(new RolePermissionConfig(tenantId, roleId, permissionId, OrganizationScope.ALL));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        RolePermissionConfig loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getRoleId()).isEqualTo(roleId);
        assertThat(loaded.getGrantedScope()).isEqualTo(OrganizationScope.ALL);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("findByTenantIdAndRoleId returns the tenant's overrides for a role")
    void findByTenantIdAndRoleId_returnsOverrides() {
        String tenantId = persistTenant();
        String roleId = persistSystemRole();
        String permissionId = seededPermissionId();
        adapter.save(new RolePermissionConfig(tenantId, roleId, permissionId, OrganizationScope.OWN));
        flushAndClear();

        assertThat(adapter.findByTenantIdAndRoleId(tenantId, roleId)).hasSize(1);
        assertThat(adapter.findByTenantIdAndRoleId(tenantId, "no-such-role")).isEmpty();
    }

    @Test
    @DisplayName("TenantScopedSpecification filters by tenant")
    void tenantScopedSpecification_filtersByTenant() {
        String tenantId = persistTenant();
        String roleId = persistSystemRole();
        String permissionId = seededPermissionId();
        adapter.save(new RolePermissionConfig(tenantId, roleId, permissionId, OrganizationScope.BRANCH));
        flushAndClear();

        Specification<RolePermissionConfigEntity> inTenant =
                new TenantScopedSpecification<RolePermissionConfigEntity>().withTenantId(tenantId).notDeleted();
        assertThat(jpaRepository.findAll(inTenant)).hasSize(1);

        Specification<RolePermissionConfigEntity> otherTenant =
                new TenantScopedSpecification<RolePermissionConfigEntity>().withTenantId("no-such-tenant").notDeleted();
        assertThat(jpaRepository.findAll(otherTenant)).isEmpty();
    }
}
