package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enums.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.model.RolePermission;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.RolePermissionEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.RolePermissionJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.EntitySpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({RolePermissionRepositoryAdapter.class, RolePermissionPersistenceMapperImpl.class})
@DisplayName("RolePermissionRepositoryAdapter — persistence round-trip (system grants)")
class RolePermissionRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private RolePermissionRepositoryAdapter adapter;

    @Autowired
    private RolePermissionJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id and grant scope")
    void saveThenReload_preservesIdentityAndAudit() {
        String roleId = persistSystemRole();
        String permissionId = seededPermissionId();
        RolePermission saved = adapter.save(new RolePermission(roleId, permissionId, OrganizationScope.BRANCH));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        RolePermission loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getRoleId()).isEqualTo(roleId);
        assertThat(loaded.getPermissionId()).isEqualTo(permissionId);
        assertThat(loaded.getGrantedScope()).isEqualTo(OrganizationScope.BRANCH);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("findByRoleId returns the role's grants")
    void findByRoleId_returnsGrants() {
        String roleId = persistSystemRole();
        String permissionId = seededPermissionId();
        adapter.save(new RolePermission(roleId, permissionId, OrganizationScope.OWN));
        flushAndClear();

        assertThat(adapter.findByRoleId(roleId)).hasSize(1);
        assertThat(adapter.findByRoleId("no-such-role")).isEmpty();
    }

    @Test
    @DisplayName("BaseSpecification filters by createdBy")
    void specification_filtersByCreatedBy() {
        String roleId = persistSystemRole();
        String permissionId = seededPermissionId();
        adapter.save(new RolePermission(roleId, permissionId, OrganizationScope.ALL));
        flushAndClear();

        Specification<RolePermissionEntity> mine =
                new EntitySpecification<RolePermissionEntity>().notDeleted().withCreatedBy("test-user");
        assertThat(jpaRepository.findAll(mine)).hasSize(1);
    }
}
